const request = require("supertest");
import * as fs from 'fs';

import app from '../../src/app'
import config from '../../src/config/config';
import path from 'path';

// inject the fake rss feed somehow


describe('Item Controller', () => {
    test("rss feed route returns file data exactly", done => {
        const rssFilePath: string = path.join(config.pathRoot, config.rssFileName)
        fs.readFile(rssFilePath, 'utf-8', (err: NodeJS.ErrnoException | null, data: string) => {
            if (err) {
                console.error('Error reading file:', err);
                return;
            }
            request(app)
            .get("/api/feed/rss")
            .expect(data)
            .expect(200, done);        
        });
    });
  });

