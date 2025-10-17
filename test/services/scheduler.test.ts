import * as path from 'path';

import config from '../../src/config/config';
import { testTextFileName, testResourcesPath } from '../testConsts';
import { Scheduler } from '../../src/services/scheduler';

const filePath = path.join(config.pathRoot, testResourcesPath, testTextFileName); 

// 		return this.count === 0 ||
		// this.episodeWatcher.changesSinceXmlGenerated;


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
        expect(scheduler.episodeWatcher.changesSinceXmlGenerated).toBe(false);

        scheduler.episodeWatcher.changesSinceXmlGenerated = true;
        expect(scheduler.episodeWatcher.changesSinceXmlGenerated);

        expect(scheduler.count == 0);
        expect(scheduler.evaluateParseConditions());
        })
})

