// endpoint to kick off a parse/write job
import { Request, Response, NextFunction } from 'express';
import * as fs from 'fs';

import { Show } from '../models/show';
import { Episode } from '../models/episode';
import { rssFilePath } from '../utils/consts';
import { generateRssXml } from '../services/rssXmlGenerator';

export const parse = (req: Request, res: Response, next: NextFunction) => {
    try {

      // todo - check for a source
      // todo - parse the source document

      // // todo test - generate show obj from static
      let show: Show = new Show("Time Crisis", "https://timecrisis.ctkhosting.net/rss", "This show rules");
      let episode: Episode = new Episode();
      show.episodes = [episode];
      // // todo test - generate rss from show obj

      fs.writeFileSync(rssFilePath, generateRssXml(show));

      res.status(200).send('Parsing files and generating new xml');
    } catch (error) {
      next(error);
    }
  };