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

        let show: Show = new Show("", "", "");
        show = this.parseShow('show.txt');

        // list out files
        fs.readdir(this.episodesPath, async (err, files) => {
            for await (const fileName of files) {
                // will also include directory names
                // check if file is mp3
                if (fileName.endsWith('.mp3')) {
                    show.episodes.push(await this.parseEpisode(fileName));
                    console.log("pushed episode ", fileName);
                }
            };

        });

        return show;
    }

    async parseEpisode(filename: string): Promise<Episode> {
        let episode: Episode = new Episode();

        // read their metadata
        const filePath = path.join(this.episodesPath, filename);
        const stats = fs.statSync(filePath);
        const lastModifiedDate: Date = stats.mtime;
        await mm.parseFile(filePath).then((metadata: any) => {
            episode.description = metadata.common.description[0];
            // write data as episode
            episode.date = lastModifiedDate;
            episode.title = filename.replace('.mp3','');

            // todo - episode image ?? common.picture. how do I extract this??? maybe extract on parse

            // todo - use inspect to pull show metadata etc for mp3s
            // todo - link episode photo if exists in dir. or extract from mp3
            // todo - set up structure for show data in a txt file            
        });
        return episode;
    };

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