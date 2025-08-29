import * as path from 'path';
import * as fs from 'fs';

import { episodesPath } from '../utils/consts';

// watch for changes
// save something for scheduler to call when it runs
// todo - mvp+ store show/ep json + cross ref before updating rss

export class DirectoryWatcher {
    public changesSinceXmlGen:boolean;

    constructor() {
        this.changesSinceXmlGen = false;
    }

    public watcher = fs.watch(episodesPath, (eventType, filename) => {
        console.log("file: " + filename + ", event: " + eventType);
        this.changesSinceXmlGen = true;
        })
}

