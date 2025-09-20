import * as path from 'path';

import config from '../../src/config/config';
import { testTextFileName, testResourcesPath } from '../testConsts';
import { Scheduler } from '../../src/services/scheduler';
import { parseAndWriteXml } from '../../src/controllers/parseController';

// inject the fake rss feed somehow

const filePath = path.join(config.pathRoot, testResourcesPath, testTextFileName); 

describe('scheduler', () => {
    test("runs parse on startup as expected", () => {
        // initialize objects
        let scheduler: Scheduler = new Scheduler();

        expect(scheduler.count == 0);
        expect(scheduler.evaluateParseConditions());
    })
})

// todo - runs when changes happen
describe('scheduler', () => {
    test("runs parse after changes made", () => {
        // todo - flip the change toggle
        let scheduler: Scheduler = new Scheduler();
        scheduler.episodeWatcher.changesSinceXmlGenerated = true;
        scheduler.count = 1;

        expect(scheduler.count == 0);
        expect(scheduler.episodeWatcher.changesSinceXmlGenerated);
        expect(scheduler.evaluateParseConditions());

        
        // todo - and set changes boolean TRUE
        // if (this.count === 0 ||
		// 	this.episodeWatcher.changesSinceXmlGenerated) {

        // re use same mock logic from above
    })
})

