import * as path from 'path';
import * as fs from 'fs';
import config from '../config/config';


// watch for changes
// save something for scheduler to call when it runs
// todo - mvp+ store show/ep json + cross ref before updating rss

export class DirectoryWatcher {
    public changesSinceXmlGenerated:boolean = false;
    // todo - pass in or set in config?
    directoryPath: string;
    public watcher;

    constructor() {
        this.changesSinceXmlGenerated = false;
        this.directoryPath = path.join(config.pathRoot, config.episodesDir);
        console.log("watching: " + this.directoryPath);
        this.watcher = this.buildWatcher();
    }

    public closeWatcher() {
        () => this.watcher?.close();
    }


    private buildWatcher() {
        // todo - will need to test watcher at some point
        console.log("watch disabled? " + config.disableWatch);
        if (!config.disableWatch) {
            console.log("creating watcher for: " + this.directoryPath)
            return fs.watch(this.directoryPath, (eventType, filename) => {
                console.log("file: " + filename + ", event: " + eventType);
                this.changesSinceXmlGenerated = true;
                console.log("flipped boolean: " + this.changesSinceXmlGenerated)
                })
            }
    }
}

