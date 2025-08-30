import { CronJob } from 'cron';
import { parseAndWriteXml } from '../controllers/parseController';
import { DirectoryWatcher } from './directoryWatcher';

// runs cron jobs to schedule xml rebuilds


// check dir watcher
// if changed, run parse
// todo - mvp+ store show/ep json + cross ref before updating rss

export class Scheduler {
	episodeWatcher:DirectoryWatcher;

	constructor() {
        this.episodeWatcher = new DirectoryWatcher;
		// console.log("checking bool: ", this.episodeWatcher.changesSinceXmlGen);
    }

	

	
	public episodeParseCron = this.generateCronJob();

	private evaluateParseConditions(): void {
		// check dir watcher
		console.log("checking for changes");
		if (this.episodeWatcher.changesSinceXmlGen) {
			parseAndWriteXml();
			this.episodeWatcher.changesSinceXmlGen = false;
		}
	}

	private generateCronJob() {
		if (process.env.NODE_ENV !== 'test') {
			return new CronJob(
				'*/15 * * * * *', // cronTime
				() => this.evaluateParseConditions(), // onTick - use arrow function to preserve 'this' context
				() => {
					console.log('Cron job running in non-test environment');
				  }, // onComplete
				true, // start
				'America/Chicago' // timeZone
			);
		}  
	}
}



