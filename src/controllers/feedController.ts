// endpoint to retrieve generated xml
import { Request, Response, NextFunction } from 'express';
import path from 'path';
import config from '../config/config';
// todo - is there a better way to jump up by 2 parent dirs? prob no

const rssFilePath: string = path.join(config.pathRoot, config.rssFileName)

export const feed = (req: Request, res: Response, next: NextFunction) => {
    try {
      // todo - cache the file? is that worth it?
      res.sendFile(rssFilePath);
    } catch (error) {
      next(error);
    }
  };
