import * as path from 'path';
import * as fs from 'fs';
// const mm = import('music-metadata');
var ffmetadata = require("ffmetadata");

import { Show } from "../../models/show";
import { Parser } from "./parser";
import { Episode } from '../../models/episode';
import { artworkPath, episodesPath, } from '../../utils/consts';


// implementation of parser for local dir

// future todos
// only update for new files/metadata. keep a diff or hash?

export class FileSystemParser implements Parser
{
    // dir structure:
    // - episodes               contains episode files
    //  L episode1.mp3          actual episode contents
    //  L show.txt              contains show details
    // - artwork                contains episode art
    //  L episode1.jpeg         stripped from episode1

    /// todo - move to constants util file

    public async parse(): Promise<Show> {
        // todo - save a log of episodes or metadata missing or without match
        // list of string pairs. episode -> string list holding fields
        // append to string

        let episodes: Episode[] = [];

        const fileNames = fs.readdirSync(episodesPath);

        // Build an array of parse promises (only .mp3 files)
        const parsePromises = fileNames
            .filter((fileName) => fileName.endsWith('.mp3'))
            .map(async (fileName) => {
                const filePath = path.join(episodesPath, fileName);
                const episode = await this.parseEpisode(filePath);
                // artwork after we know the title
                this.extractEpisodeArtwork(filePath, episode.title);
                return episode;
            });
        // wait for all episodes to finish parsing
        episodes = await Promise.all(parsePromises);

        let show: Show = this.parseShow('show.txt', episodes);

        // todo - create artwork dir if not exists
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
            console.log("pulled image: ", data);
        });
    }

    generateCoverPath(artPath: string): any {
        let path = artworkPath + artPath + '.jpeg';
        console.log("artPath: ", path)
        return {
            coverPath: path
        };
    }

    parseShow(filename: string, episodes: Episode[]): Show {
        const filePath = path.join(episodesPath, filename);

        const showJson = fs.readFileSync(filePath, 'utf8');
        let show: Show = JSON.parse(showJson);
        show.episodes = episodes;

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