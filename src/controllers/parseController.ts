// endpoint to kick off a parse/write job
import { Request, Response, NextFunction } from 'express';
import * as path from 'path'

import { FileSystemParser } from '../services/parse/filesystemParser';
import { RssBuilder } from '../services/rssBuilder';
import { FileWriter } from '../services/fileWriter';
import config from '../config/config';


let filesystemParser: FileSystemParser = new FileSystemParser;
let fileWriter: FileWriter = new FileWriter;
let xmlGenerator: RssBuilder = new RssBuilder;

let episodesLocation: string = path.join(config.pathRoot, config.episodesDir)
let rssFileLocation: string = path.join(config.pathRoot, config.rssFileName)


export const parseLocal = async (req: Request, res: Response, next: NextFunction) => {

    try {
      parseAndWriteXml();
      res.status(200).send('Parsing files and generating new xml');
    } catch (error) {
      next(error);
    }
  };

  // todo - extract into a service. or parser
  export const parseAndWriteXml = async () => {
    const show = await filesystemParser.parse(episodesLocation);
    fileWriter.writeFile(rssFileLocation, xmlGenerator.generateRss(show));
  }


// todo - url based parser
// parse the source document (if url)
