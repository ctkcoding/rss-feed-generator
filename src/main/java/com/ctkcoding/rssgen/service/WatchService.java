package com.ctkcoding.rssgen.service;

import com.ctkcoding.rssgen.model.Show;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// todo - enable scheduling annotation? or add that to the
@Slf4j
@Component
public class WatchService {
    volatile AtomicBoolean newFileChanges = new AtomicBoolean(false);
    volatile AtomicInteger failureCounter = new AtomicInteger(0);

    private static final Logger logger = LoggerFactory.getLogger(WatchService.class);

    @Autowired
    ParseService parseService;

    @Autowired
    RssService rssService;

    private Integer failureLimit = 5;

    // todo - watch service should watch the episodes directory that parse service reads episodes file from
    // todo - if new files, deleted files, or changes to existing files, set value of newFileChanges to true

    @Scheduled(cron = "0 * * * * *")
    public void checkForNewChanges() {
        if (newFileChanges.get()) {
            logger.info("New file changes found. Kicking off parse and write");
            Show show = parseService.generateShow();
            try {
                String path = rssService.writeRss(show);
                logger.info("RSS feed written to: {}", path);
                newFileChanges.set(false);
            } catch (Exception e) {
                logger.error("Failed to generate RSS feed: {}", e.getMessage(), e);
                failureCounter.incrementAndGet();
                if (failureCounter.get() > failureLimit); {
                    newFileChanges.set(false);
                    logger.error("Reached RSS feed generation failure limit of 5. Ending retries.");
                }
            }
        } else {
            logger.info("No new changes found to files. Napping for a minute!");
        }
    }
}
