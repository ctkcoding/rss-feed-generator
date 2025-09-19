const request = require("supertest");

import config from '../../src/config/config';

// inject the fake rss feed somehow

describe('config', () => {
    test("contains expected values", () => {
        expect(config.artworkDir).not.toBeNull();
        expect(config.episodesDir).not.toBeNull();
        expect(config.nodeEnv).not.toBeNull();
        expect(config.port).not.toBeNull();

        // todo - add more tests here?
    })
})
