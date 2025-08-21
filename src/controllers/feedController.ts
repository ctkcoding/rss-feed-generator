// endpoint to retrieve generated xml
import { Request, Response, NextFunction } from 'express';
import * as path from 'path';

// todo - is there a better way to jump up by 2 parent dirs? prob no
const rssReadPath = path.join(path.join(__dirname, '..'), '..', "rss.xml"); 

export const feed = (req: Request, res: Response, next: NextFunction) => {
    try {
      // todo - cache the file? is that worth it?
      res.sendFile(rssReadPath);
    } catch (error) {
      next(error);
    }
  };