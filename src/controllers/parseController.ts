// endpoint to kick off a parse/write job
import { Request, Response, NextFunction } from 'express';

import { FileSystemParser } from '../services/parser/filesystemParser';
import { generateRssXml } from '../services/rssXmlGenerator';
import { FileWriter } from '../services/fileWriter';


let filesystemParser: FileSystemParser = new FileSystemParser;
let fileWriter: FileWriter = new FileWriter;

export const parseLocal = async (req: Request, res: Response, next: NextFunction) => {

    try {
      parseAndWriteXml();
      res.status(200).send('Parsing files and generating new xml');
    } catch (error) {
      next(error);
    }
  };

  export const parseAndWriteXml = async () => {
    const show = await filesystemParser.parse();
    fileWriter.writeFile(generateRssXml(show));
  }


// todo - url based parser
// parse the source document (if url)
