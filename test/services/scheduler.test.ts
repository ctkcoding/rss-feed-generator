import * as path from 'path';

import config from '../../src/config/config';
import { testTextFileName, testResourcesPath } from '../testConsts';
import { Scheduler } from '../../src/services/scheduler';

// inject the fake rss feed somehow

const filePath = path.join(config.pathRoot, testResourcesPath, testTextFileName); 


describe('scheduler', () => {
    test("runs schedule as expected", () => {
        // initialize objects
        let scheduler: Scheduler = new Scheduler();

        // set values
        // todo - DONT use real watcher!!

        // run test function
        // todo - increment something
        // if (this.count === 0 ||
		// 	this.episodeWatcher.changesSinceXmlGenerated) {


        // check values
        // MOCK on writing XML
    })
})
