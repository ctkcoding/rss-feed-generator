import * as path from 'path';
import * as fs from 'fs';
// const mm = import('music-metadata');
var ffmetadata = require("ffmetadata");

import { Show } from "../../models/show";
import { Parser } from "./parser";
import { Episode } from '../../models/episode';
import config from '../../config/config';
import { artworkFileFormat, episodeFileFormat } from '../../utils/consts';


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
                const episode = await this.parseEpisode(filePath);
                // artwork after we know the title
                // todo - split into a parallel loop as n ->

                // todo - allow disabling generating artwork
                await this.extractEpisodeArtwork(filePath, path.join(artworkPath, episode.title));
                return episode;
            });
        // wait for all episodes to finish parsing
        show.episodes = await Promise.all(parsePromises);


        // todo - create artwork dir if not exists
        // note - this should be a mounted dir so it always exists
        // unless strip artwork flag turned off

        return show;
    }

    parseEpisode(filePath: string): Promise<Episode> {
        return new Promise(async (resolve) => {
            let episode: Episode = new Episode();

            // read their metadata
            const stats = fs.statSync(filePath);
            const lastModifiedDate: Date = stats.mtime;

            await ffmetadata.read(filePath, (err: any, data: any) => {
                /*
                    data solo:  {
                    title: '001 Episode 1',
                    track: '1',
                    album: 'Time Crisis Season 1',
                    TIT3: 'Ezra kicks off his first episode with a full house.',
                    TGID: 'Time Crisis',
                    TDES: 'Ezra kicks off his first episode with a full house.',
                    date: '2015',
                    encoder: 'Lavf61.7.100'
                    }
                */
                episode.description = data.TDES;
                // write data as episode
                episode.date = lastModifiedDate;

                // todo - set from file name or metadata
                episode.title = data.title;

                // todo - link url generated

                console.log("ep data: ", episode);
                resolve(episode);
            });
        });
    };

    async extractEpisodeArtwork(episodePath: string, artPath: string) {
        // todo - check if art exists before generating?
        // todo - TTL on art age?
        await ffmetadata.read(episodePath, this.generateCoverPath(artPath), function(err:any , data:any ) {
            // console.log("pulled image: ", data);
        });
    }

    generateCoverPath(artPath: string): any {
        let coverPathValue: string = artPath + artworkFileFormat;
        console.log("coverPathValue: " +  coverPathValue);
        return {
            coverPath: coverPathValue
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