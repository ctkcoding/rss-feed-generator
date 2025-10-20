// endpoint to retrieve generated xml
import { Request, Response, NextFunction } from 'express';
import path from 'path';
import { sanitize } from "sanitize-filename-ts";

import config from '../config/config';
// todo - is there a better way to jump up by 2 parent dirs? prob no

export const episodes = (req: Request, res: Response, next: NextFunction) => {
  const filename = req.params.filename;

  try {
    // todo - cache the file? is that worth it?
    res.sendFile(path.join(config.pathRoot,path.join(config.episodesDir,sanitize(filename))));
  } catch (error) {
    next(error);
  }
  };


