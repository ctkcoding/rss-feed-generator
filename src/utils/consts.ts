import * as path from 'path';
import config from '../config/config';

export const rssFileName = "rss.xml";
// xml write path
export const rssFilePath = path.join(__dirname, '..', '..', rssFileName); 

// episodes root
export const episodesPath = path.join(__dirname, '..', '..', config.episodesDir); 

console.log("const for eps: " + episodesPath);


// test consts
// todo - move these to /test when I build that
export const testXmlData = "this is xml";
