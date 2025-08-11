const request = require("supertest");

import app from '../../src/app'

// inject the fake rss feed somehow

describe('Item Controller', () => {
    test("rss feed route works", done => {
    request(app)
        .get("/api/feed/rss")
        .expect('Here is some xml')
        .expect(200, done);
    });
  });

