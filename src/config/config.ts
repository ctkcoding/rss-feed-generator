import dotenv from 'dotenv';
import * as path from 'path';

import { rssFileNameDefault, episodesDirDefault, artworkDirDefault, showFileNameDefault } from '../utils/consts';


dotenv.config();

interface Config {
  port: number;
  nodeEnv: string;
  disableWatch: boolean;
  extractArtwork: boolean;

  pathRoot: string;
  episodesDir: string;
  artworkDir: string;
  showFileName: string;
  rssFileName: string;
}

const config: Config = {
  port: Number(process.env.PORT) || 3000,
  nodeEnv: process.env.NODE_ENV || 'development',
  disableWatch: process.env.NODE_ENV === 'test',
  extractArtwork: process.env.EXTRACT_ARTWORK === "true",

  pathRoot: process.env.PATH_ROOT || path.join(__dirname, '..', '..'),
  episodesDir: process.env.EPISODES_DIR || episodesDirDefault,
  artworkDir: process.env.ARTWORK_DIR || artworkDirDefault,
  showFileName: process.env.SHOW_FILENAME || showFileNameDefault,
  rssFileName: process.env.RSS_FILENAME || rssFileNameDefault
};

export default config;