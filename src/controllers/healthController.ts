import { Request, Response, NextFunction } from 'express';

export const health = (req: Request, res: Response, next: NextFunction) => {
    try {
        res.status(200).send('Up and running!' );
    } catch (error) {
      next(error);
    }
  };