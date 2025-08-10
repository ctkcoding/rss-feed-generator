// endpoint to kick off a parse/write job
import { Request, Response, NextFunction } from 'express';

export const parse = (req: Request, res: Response, next: NextFunction) => {
    try {
        res.status(200).send('Parsing files and generating new xml');
    } catch (error) {
      next(error);
    }
  };