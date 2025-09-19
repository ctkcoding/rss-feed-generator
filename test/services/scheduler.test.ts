import * as path from 'path';

import config from '../../src/config/config';
import { testTextFileName, testResourcesPath } from '../testConsts';
import { Scheduler } from '../../src/services/scheduler';

// inject the fake rss feed somehow

const filePath = path.join(config.pathRoot, testResourcesPath, testTextFileName); 


describe('scheduler', () => {
    test("runs schedule on startup as expected", () => {
        // initialize objects
        let scheduler: Scheduler = new Scheduler();

        // set values
        // todo - DONT use real watcher!!

        // run test function
        // todo - test that it runs on startup

        // if (this.count === 0 ||
		// 	this.episodeWatcher.changesSinceXmlGenerated) {


        // check values
        // MOCK on writing XML
    })
})

// todo - runs when changes happen
// todo - increment the counter
// todo - and set changes boolean TRUE
        // if (this.count === 0 ||
		// 	this.episodeWatcher.changesSinceXmlGenerated) {

// re use same mock logic from above
