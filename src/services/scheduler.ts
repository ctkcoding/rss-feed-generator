import { CronJob } from 'cron';
import { parseAndWriteXml } from '../controllers/parseController';
import { Watcher } from './directoryWatcher';

// runs cron jobs to schedule xml rebuilds


// check dir watcher
// if changed, run parse
// todo - mvp+ store show/ep json + cross ref before updating rss

export class Scheduler {

	episodeWatcher:Watcher;

	constructor(watcher:Watcher) {
        this.episodeWatcher = watcher;
		// console.log("checking bool: ", this.dirWatcher.changesSinceXmlGen);
    }

	
	public episodeParseCron = new CronJob(
		'*/15 * * * * *', // cronTime
		this.evaluteParseConditions, // onTick
		null, // onComplete
		true, // start
		'America/Chicago' // timeZone
	);

	private evaluteParseConditions():void {
		// check dir watcher
		console.log("checking for changes");
		if (this.episodeWatcher.changesSinceXmlGen) {
			parseAndWriteXml();
			this.episodeWatcher.changesSinceXmlGen = false;
		}
	}
}



