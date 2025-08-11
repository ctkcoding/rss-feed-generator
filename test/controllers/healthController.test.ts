const request = require("supertest");

import app from '../../src/app'

describe('Item Controller', () => {
    test("health advice route works", done => {
    request(app)
        .get("/api/health/basic")
        .expect('Up and running!')
        .expect(200, done);
    });
  });

