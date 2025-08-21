import * as path from 'path';
import * as fs from 'fs';
const mm = require('music-metadata');

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

        let episodes: Episode[] = [];
        let show: Show = new Show("", "", "");

        // list out files
        fs.readdirSync(this.episodesPath).forEach( async(filename) => {
            // will also include directory names
            console.log(filename);
            // check if file is mp3
            if (filename.endsWith('.mp3')) {
                console.log("it's an mp3! parsing :)", filename);
                episodes.push(this.parseEpisode(filename));
                console.log("temp episodes length: ", episodes.length);
            }

            if (filename === ('show.txt')) {
                console.log('parsing show details', filename);
                show = this.parseShow(filename);
            }
    
        });

        // saves into episode.ts type
        // todo - parallel if possible a la java stream
        show.episodes = episodes;

        console.log("episode count: ", show.episodes.length)

        return show;
    }

    parseEpisode(filename: string): Episode {
        // read their metadata
        const filePath = path.join(this.episodesPath, filename);
        console.log("path: ", filePath);
        const stats = fs.statSync(filePath);
        // sorry! its json tho
        const metadata: any = Promise.resolve(mm.parseFile(filePath));
        const lastModifiedDate: Date = stats.mtime; // or stats.mtimeMs for milliseconds

        // write data as episode             
        let episode: Episode = new Episode();
        episode.date = lastModifiedDate;
        episode.description = metadata['common']['description'][0];
        episode.title = filename.replace('.mp3','');

        // todo - episode image ?? common.picture. how do I extraxct this??? maybe extract on parse
        // add to episodes list. NOT SHOW'S EPISODES
        // show.episodes = [episode];

        // todo - use inspect to pull show metadata etc for mp3s
        // todo - link episode photo if exists in dir. or extract from mp3
        // todo - set up structure for show data in a txt file
        return episode;
    };
    parseShow(filename: string): Show {
        const filePath = path.join(this.episodesPath, filename);
        console.log("path: ", filePath);

        const showJson = fs.readFileSync(filePath, 'utf8');
        let show: Show = JSON.parse(showJson);
        console.log(show);

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