// endpoint to kick off a parse/write job
import { Request, Response, NextFunction } from 'express';
import * as path from 'path';
import * as fs from 'fs';
const mm = require('music-metadata');
import { inspect } from 'node:util';

//todo - delete these!
import { Show } from '../models/show';
import { Episode } from '../models/episode';
import { rssFilePath } from '../utils/consts';
import { generateRssXml } from '../services/rssXmlGenerator';

const episodesPath = path.join(path.join(__dirname, '..'), '..', "/episodes"); 

export const parseLocal = async (req: Request, res: Response, next: NextFunction) => {
    try {
      // check for a source if url based. BREAK INTO TWO APIS
      // parse the source document (if url)




      // todo - move logic to dir parser + replace w call to parser
      // list out files
      fs.readdirSync(episodesPath).forEach( async(filename) => {
        // will also include directory names
        console.log(filename);
        // todo - check if file is mp3
        if (filename.endsWith('.mp3')) {
          // todo - read their metadata
          // todo - write data as episode 
          console.log("it's an mp3! parsing :)")

          const metadata = await mm.parseFile(path.join(episodesPath, filename));

          // Output the parsed metadata to the console in a readable format
          console.log(inspect(metadata, { showHidden: false, depth: null }));
      
        }

      });

      // let show: Show = new Show("Time Crisis", "https://timecrisis.ctkhosting.net/rss", "This show rules");
      // let episode: Episode = new Episode();
      // show.episodes = [episode];

      res.status(200).send('Parsing files and generating new xml');
    } catch (error) {
      next(error);
    }
  };