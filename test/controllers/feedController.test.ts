const request = require("supertest");
import * as fs from 'fs';

import app from '../../src/app'
import { rssFilePath } from '../../src/utils/consts';

// inject the fake rss feed somehow


describe('Item Controller', () => {
    test("rss feed route works", done => {
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

