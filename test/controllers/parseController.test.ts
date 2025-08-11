const request = require("supertest");

import app from '../../src/app'

// inject the fake rss feed somehow

describe('Item Controller', () => {
    test("rss feed route works", done => {
    request(app)
        .get("/api/parse/run")
        .expect('Parsing files and generating new xml')
        .expect(200, done);
    });
  });

