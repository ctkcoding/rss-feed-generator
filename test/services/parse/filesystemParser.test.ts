import * as path from 'path';
import * as fs from 'fs';

import config from '../../../src/config/config';
import { testResourcesPath, testEpisodeFileName } from '../../testConsts';
import { FileSystemParser } from '../../../src/services/parse/filesystemParser';

const episodesPath = path.join(config.pathRoot, testResourcesPath, config.episodesDir); 
const mp3Path = path.join(episodesPath, testEpisodeFileName);
const artworkPath = path.join(config.pathRoot, testResourcesPath, config.artworkDir)

describe('filesystemParser', () => {
    test("parses dir into expected rss", async () => {

        let parser: FileSystemParser = new FileSystemParser();

        // todo - create sample show json
        // todo - create sample mp3 w tags
        // todo - pass in sample directory
        // todo - 
        let rss = await parser.parse(episodesPath).then(
            (data) => {

        // todo - clean up magic strings
        expect(data.title).toBe("Time Crisis");
        expect(data.description).toBe("This show rules")
        expect(data.link).toBe("https://timecrisis.apple.com")
        expect(data.episodes[0].title).toBe("Test title");
        expect(data.episodes[0].description).toBe("This is the description");
        expect(data.episodes[0].pubdate).toStrictEqual(fs.statSync(mp3Path).mtime);
        // expect(data.episodes[0].author).toBe("author");

        expect(fs.access(artworkPath, () => {}));
    });


    })
})
