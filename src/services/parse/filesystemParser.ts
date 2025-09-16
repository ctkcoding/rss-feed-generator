import * as path from 'path';
import * as fs from 'fs';
// const mm = import('music-metadata');
var ffmetadata = require("ffmetadata");

import { Show } from "../../models/show";
import { Parser } from "./parser";
import { Episode } from '../../models/episode';
import config from '../../config/config';


// implementation of parser for local dir

// future upgrades
// only update for new files/metadata. keep a diff or hash?

// dir structure:
// - episodes               contains episode files
//  L episode1.mp3          actual episode contents
//  L show.txt              contains show details
// - artwork                contains episode art
//  L episode1.jpeg         stripped from episode1

export class FileSystemParser implements Parser
{
    public async parse(episodesSource: string): Promise<Show> {
        // todo - save a log of episodes or metadata missing or without match
        // list of string pairs. episode -> string list holding fields
        // append to string

        // todo - make configurable?
        const artworkPath = path.join(episodesSource, '..', config.artworkDir);

        let show: Show = this.parseShow(episodesSource, 'show.txt');

        const fileNames = fs.readdirSync(episodesSource);

        // Build an array of parse promises (only .mp3 files)
        const parsePromises = fileNames
            .filter((fileName) => fileName.endsWith('.mp3'))
            .map(async (fileName) => {
                const filePath = path.join(episodesSource, fileName);
                const episode = await this.parseEpisode(filePath);
                // artwork after we know the title
                // todo - split into a parallel loop as n ->
                this.extractEpisodeArtwork(filePath, artworkPath + episode.title);
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
        return new Promise((resolve) => {
            let episode: Episode = new Episode();

            // read their metadata
            const stats = fs.statSync(filePath);
            const lastModifiedDate: Date = stats.mtime;

            ffmetadata.read(filePath, (err: any, data: any) => {
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
                episode.title = data.title;

                // todo - link url generated

                console.log("ep data: ", episode);
                resolve(episode);
            });
        });
    };

    extractEpisodeArtwork(episodePath: string, artPath: string) {
        // todo - test when no art exists
        ffmetadata.read(episodePath, this.generateCoverPath(artPath), function(err:any , data:any ) {
            // console.log("pulled image: ", data);
        });
    }

    generateCoverPath(artPath: string): any {
        let path = artPath + '.jpeg';
        console.log("artPath: ", path)
        return {
            coverPath: path
        };
    }

    parseShow(episodesPath: string, filename: string): Show {
        const filePath = path.join(episodesPath, filename);

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