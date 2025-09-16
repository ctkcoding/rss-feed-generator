import dotenv from 'dotenv';

dotenv.config();

interface Config {
  port: number;
  nodeEnv: string;
  episodesDir: string;
  artworkDir: string;
  disableWatch: boolean;
}

const config: Config = {
  port: Number(process.env.PORT) || 3000,
  nodeEnv: process.env.NODE_ENV || 'development',
  episodesDir: process.env.EPISODES_DIR || '/episodes/',
  artworkDir: process.env.ARTWORK_DIR || '/artwork/',
  disableWatch: process.env.NODE_ENV === 'test',
};

export default config;