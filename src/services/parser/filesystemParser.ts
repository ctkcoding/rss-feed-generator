import * as path from 'path';
import * as fs from 'fs';
// const mm = import('music-metadata');
var ffmetadata = require("ffmetadata");

import { Show } from "../../models/show";
import { Parser } from "./parser";
import { Episode } from '../../models/episode';


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
    private episodesPath = path.join(path.join(path.join(__dirname, '..'), '..'), '..', "/episodes"); 
    private artworkPath = path.join(path.join(path.join(__dirname, '..'), '..'), '..', "/artwork/"); 

    public parse(): Show {
        // todo - save a log of episodes or metadata missing or without match
        // list of string pairs. episode -> string list holding fields
        // append to string

        let episodes: Episode[] = [];

        // list out files
        fs.readdirSync(this.episodesPath).forEach( fileName => {
            // will also include directory names
            // check if file is mp3
            if (fileName.endsWith('.mp3')) {
                const filePath = path.join(this.episodesPath, fileName);
                let episode: Episode = this.parseEpisode(filePath);
                episodes.push(episode);
                console.log("pushed episode ", episode);

                // todo - include with metadata parsing?
                this.extractEpisodeArtwork(filePath, episode.title);
            }
        });

        let show: Show = this.parseShow('show.txt', episodes);

        // todo - create artwork dir if not exists
        // unless strip artwork flag turned off



        return show;
    }

    parseEpisode(filePath: string): Episode {
        let episode: Episode = new Episode();

        // read their metadata
        const stats = fs.statSync(filePath);
        const lastModifiedDate: Date = stats.mtime;

        ffmetadata.read(filePath, function(err:any , data:any ) {
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
        });

        // todo - link episode photo if exists in dir. or extract from mp3
        return episode;
    };

    extractEpisodeArtwork(episodePath: string, artPath: string) {
        // todo - test when no art exists
        ffmetadata.read(episodePath, this.generateCoverPath(artPath), function(err:any , data:any ) {
            console.log("pulled image: ", data);
        });
    }

    generateCoverPath(artPath: string): any {
        let path = this.artworkPath + artPath + '.jpeg';
        console.log("artPath: ", path)
        return {
            coverPath: path
          };
    }

    parseShow(filename: string, episodes: Episode[]): Show {
        const filePath = path.join(this.episodesPath, filename);

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