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
    // takes a directory to watch
    // dir structure:
    // - show.txt           contains show details
    // - episodes               contains episode files
    //  L episode1.mp3          actual episode contents
    //  L episode1.txt          contains episode details

    private episodesPath = path.join(path.join(path.join(__dirname, '..'), '..'), '..', "/episodes"); 


    public parse(): Show {
        // todo - save a log of episodes or metadata missing or without match
        // list of string pairs. episode -> string list holding fields
        // append to string

        let show: Show = new Show("", "", "");
        show = this.parseShow('show.txt');

        // list out files
        fs.readdirSync(this.episodesPath).forEach( fileName => {
            // will also include directory names
            // check if file is mp3
            if (fileName.endsWith('.mp3')) {
                let episode: Episode = this.parseEpisode(fileName);
                show.episodes.push(episode);
                console.log("pushed episode ", fileName);
            }
        });

        return show;
    }

    parseEpisode(filename: string): Episode {
        let episode: Episode = new Episode();

        // read their metadata
        const filePath = path.join(this.episodesPath, filename);
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





        ffmetadata.read(filePath, this.generateCoverPath(filename), function(err:any , data:any ) {
            console.log("pulled image: ", data);
        });

        // todo - link episode photo if exists in dir. or extract from mp3

        return episode;
    };

    generateCoverPath(fileName: string): any {
        return {
            coverPath: fileName.replace('.mp3','.jpeg'),
          };
    }

    parseShow(filename: string): Show {
        const filePath = path.join(this.episodesPath, filename);

        const showJson = fs.readFileSync(filePath, 'utf8');
        let show: Show = JSON.parse(showJson);
        show.episodes = [];

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