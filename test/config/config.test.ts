const request = require("supertest");

import config from '../../src/config/config';

describe('config', () => {
    test("contains expected values", () => {
        expect(config.port).not.toBeNull();
        expect(config.nodeEnv).not.toBeNull();
        expect(config.disableWatch).not.toBeNull();

        expect(config.pathRoot).not.toBeNull();
        expect(config.episodesDir).not.toBeNull();
        expect(config.artworkDir).not.toBeNull();
        expect(config.showFileName).not.toBeNull();
        expect(config.rssFileName).not.toBeNull();    
    })
})
