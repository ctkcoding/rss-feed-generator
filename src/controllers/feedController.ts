// endpoint to retrieve generated xml
import { Request, Response, NextFunction } from 'express';
import { rssFilePath } from '../utils/consts';
// todo - is there a better way to jump up by 2 parent dirs? prob no

export const feed = (req: Request, res: Response, next: NextFunction) => {
    try {
      // todo - cache the file? is that worth it?
      res.sendFile(rssFilePath);
    } catch (error) {
      next(error);
    }
  };
