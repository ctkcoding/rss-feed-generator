import * as path from 'path';
import * as fs from 'fs';
var ffmetadata = require("ffmetadata");

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
        const fileNames = fs.readdirSync(episodesSourceDir);
        let show: Show = this.parseShow(episodesSourceDir, config.showFileName);


        // Build an array of parse promises (only .mp3 files)
        const parsePromises = fileNames
            .filter((fileName) => fileName.endsWith(episodeFileFormat))
            .map(async (fileName) => {
                const filePath = path.join(episodesSourceDir, fileName);
                const episode = await this.parseEpisode(filePath, show.link, fileName);
                // artwork after we know the title
                // todo - split into a parallel loop as n ->

                // todo - allow disabling generating artwork
                await this.extractEpisodeArtwork(filePath, path.join(artworkPath, fileName.replace(episodeFileFormat,"")));
                return episode;
            });
        // wait for all episodes to finish parsing
        show.episodes = await Promise.all(parsePromises);


        // todo - create artwork dir if not exists
        // note - this should be a mounted dir so it always exists
        // unless strip artwork flag turned off

        return show;
    }

    parseEpisode(filePath: string, feedUrl: string, fileName: string): Promise<Episode> {
        return new Promise(async (resolve) => {
            // let episode: Episode = new Episode();

            // read their metadata
            const stats = fs.statSync(filePath);
            const lastModifiedDate: Date = stats.mtime;

            await ffmetadata.read(filePath, (err: any, data: any) => {
    
                console.log("filePath: " + filePath);
                console.log("data: " + data);

                // title: string, description: string, url: string, pubdate: Date, image: string, enclosure: string
                let episode: Episode = new Episode(
                    sanitize(data.title),
                    data.TDES,
                    feedUrl + "/episodes/" + encodeURIComponent(fileName),
                    // "show.url PLUS FILEPATH pending parse: " + filePath,
                    lastModifiedDate,
                    feedUrl + "/artwork/" + encodeURIComponent(fileName.replace(episodeFileFormat,artworkFileFormat)),
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
        let artPathOriginal: string = artPath + "_original" + artworkFileFormat;
        let artPathResized: string = artPath + artworkFileFormat;

        console.log("Checking for embedded art at " + artPathOriginal);
        if (!this.checkArtworkExists(artPathOriginal) && !this.checkArtworkExists(artPathResized)) { 
            await ffmetadata.read(episodePath, this.generateCoverPath(artPathOriginal), function(err:any , data:any ) {
                // console.log("pulled image: ", data);
            });
        }

        console.log("Check for resized art");
        if (!this.checkArtworkExists(artPathOriginal)){ 
                sharp(artPathOriginal)
                    .resize({width: 1400, height: 1400})
                    .toFile(artPathResized);
            }
    }

    checkArtworkExists(artPath: string): boolean {
        fs.access(artPath, fs.constants.R_OK, (err) => {
            if (err && err!.code === 'ENOENT') {
                console.log('Artwork not written. Proceed');
                return false
            } else if (err) {
                console.error('Error while checking if artwork exists:', err);
            } else {
                console.log('Artwork file already exists, do not rewrite.')
            }
          });
        return true;
    }

    generateCoverPath(artPathFull: string): any {
        // console.log("coverPathValue: " +  coverPathValue);
        return {
            coverPath: artPathFull
        };
    }

    parseShow(episodesPath: string, showFileName: string): Show {
        const filePath = path.join(episodesPath, showFileName);

        const showJson = fs.readFileSync(filePath, 'utf8');
        let show: Show = JSON.parse(showJson);
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
}