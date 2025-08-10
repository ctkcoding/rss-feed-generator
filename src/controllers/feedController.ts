// endpoint to retrieve generated xml
import { Request, Response, NextFunction } from 'express';

export const feed = (req: Request, res: Response, next: NextFunction) => {
    try {
        res.status(200).send('Here is some xml');
    } catch (error) {
      next(error);
    }
  };