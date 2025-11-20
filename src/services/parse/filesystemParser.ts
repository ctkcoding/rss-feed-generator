import * as path from 'path';
import * as fs from 'fs/promises';
// eslint-disable-next-line @typescript-eslint/no-require-imports
const ffmetadata = require("ffmetadata");

import { Show } from "../../models/show";
import { Parser } from "./parser";
import { Episode } from '../../models/episode';
import config from '../../config/config';
import { artworkFileFormat, episodeFileFormat } from '../../utils/consts';
import { sanitize } from 'sanitize-filename-ts';
import sharp from 'sharp';


// implementation of parser for local dir

// future upgrades
// only update for new files/metadata. keep a diff or hash?

// todo - MOVE THIS TO README
// dir structure:
// - episodes               contains episode files
//  L episode1.mp3          actual episode contents
//  L show.txt              contains show details
// - artwork                contains episode art
//  L episode1.jpeg         stripped from episode1

export class FileSystemParser implements Parser
{
    public async parse(episodesSourceDir: string): Promise<Show> {
        // todo - save a log of episodes or metadata missing or without match
        // list of string pairs. episode -> string list holding fields
        // append to string

        const artworkPath = path.join(episodesSourceDir, '..', config.artworkDir);        
        const fileNames = await fs.readdir(episodesSourceDir);
        const show: Show = await this.parseShow(episodesSourceDir, config.showFileName);

        // Build an array of parse promises (only .mp3 files)
        const parsePromises = fileNames
            .filter((fileName) => fileName.endsWith(episodeFileFormat))
            .map(async (fileName) => {
                const filePath = path.join(episodesSourceDir, fileName);
                const episode = await this.parseEpisode(filePath, show.link, fileName);
                // artwork after we know the title
                // todo - split into a parallel loop as n ->

                // todo - allow disabling generating artwork
                if (config.extractArtwork) {
                    await this.extractEpisodeArtwork(filePath, path.join(artworkPath, fileName.replace(episodeFileFormat,"")));
                }
                return episode;
            });
        // wait for all episodes to finish parsing
        show.episodes = await Promise.all(parsePromises); // episodes

        // todo - create artwork dir if not exists
        // note - this should be a mounted dir so it always exists
        // unless strip artwork flag turned off

        return show;
    }

    parseEpisode(filePath: string, feedUrl: string, fileName: string): Promise<Episode> {
        return new Promise(async (resolve) => {
            // let episode: Episode = new Episode();

            // read their metadata
            const stats = await fs.stat(filePath);
            const lastModifiedDate: Date = stats.mtime;

            await ffmetadata.read(filePath, (err: any, data: any) => {
    
                console.log("filePath: " + filePath);
                console.log("data: " + data);

                // title: string, description: string, url: string, pubdate: Date, image: string, enclosure: string
                const episode: Episode = new Episode(
                    sanitize(data.title ?? fileName),
                    data.TDES ?? "",
                    feedUrl + "/episodes/" + encodeURIComponent(fileName),
                    // "show.url PLUS FILEPATH pending parse: " + filePath,
                    lastModifiedDate,
                    config.extractArtwork ? feedUrl + "/artwork/" + encodeURIComponent(fileName.replace(episodeFileFormat,artworkFileFormat)) : '',
                    // "parse out image url like show url",
                    feedUrl + "/episodes/" + encodeURIComponent(fileName),
                );

                // episode.description = data.TDES;
                // write data as episode
                // episode.date = lastModifiedDate;

                // todo - set from file name or metadata
                // episode.title = data.title;

                // todo - link url generated

                resolve(episode);
            });
        });
    };

    async extractEpisodeArtwork(episodePath: string, artPath: string) {
        // todo - check if art exists before generating?
        // todo - TTL on art age?
        const artPathOriginal: string = artPath + "_original" + artworkFileFormat;
        const artPathResized: string = artPath + artworkFileFormat;

        console.log("Checking for embedded art at " + artPathOriginal);

        await this.checkArtworkExists(artPathOriginal).then(async (exists) => {
            if (!exists) {
                console.log("extracting mp3 artwork to: " + artPathOriginal);
                await ffmetadata.read(episodePath, this.generateCoverPath(artPathOriginal), async (err:any , data:any ) => {
                    console.log("pulled image: ", data);
                    await this.checkArtworkExists(artPathResized).then(async (exists) => {
                    if (!exists) {
                    console.log("Checking for resized art");
                    sharp(artPathOriginal)
                        .resize({width: 1400, height: 1400})
                        .toFile(artPathResized);
                    }
                    });
                });
            } else {
                await this.checkArtworkExists(artPathResized).then(async (exists) => {
                        if (!exists) {
                        console.log("Checking for resized art");
                        sharp(artPathOriginal)
                            .resize({width: 1400, height: 1400})
                            .toFile(artPathResized);
                        }
                });
            }
        })
        
    }

    async checkArtworkExists(artPath: string): Promise<boolean> {
        try {
            await fs.access(artPath);
            return true;
        } catch (error) {
            return false;
        }
    }

    generateCoverPath(artPathFull: string): any {
        // console.log("coverPathValue: " +  coverPathValue);
        return {
            coverPath: artPathFull
        };
    }

    async parseShow(episodesPath: string, showFileName: string): Promise<Show> {
        const filePath = path.join(episodesPath, showFileName);

        const showJson = await fs.readFile(filePath, 'utf8');
        const show: Show = JSON.parse(showJson);
        // show.episodes = episodes;

        // parse show.txt for show details
        // title
        // link
        // description
        // image
        // author
        // etc

        // create show from txt details
        return show;
    };

    fallbackIfNull(optional: string, fallback: string): string {
        if (optional == null) {
            return fallback;
        } else {
            return optional;
        }

    }
}