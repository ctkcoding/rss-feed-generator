package com.ctkcoding.rssgen.service;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.model.Show;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WatcherService {
    AtomicBoolean newFileChanges = new AtomicBoolean(false);
    AtomicInteger failureCounter = new AtomicInteger(0);
    AtomicBoolean successfulParse = new AtomicBoolean(false);

  private static final Logger logger = LoggerFactory.getLogger(WatcherService.class);

  @Autowired ParseService parseService;

  @Autowired RssService rssService;

  @Autowired RssConfig rssConfig;

  WatchService fileWatchService;
  Thread watchThread;
  volatile boolean running = false;

    @EventListener(ApplicationStartedEvent.class)
    void startWatching() {
     if (!Boolean.TRUE.equals(rssConfig.getFileWatch())) {
       logger.info("File watching is disabled");
       return;
     }

    Path episodesDir = Path.of(System.getProperty("user.dir")).resolve(rssConfig.getEpisodesDir());

    if (!Files.exists(episodesDir) || !Files.isDirectory(episodesDir)) {
      throw new IllegalStateException(
          "Episodes directory does not exist: "
              + episodesDir
              + ". This is required for the application to function.");
    }

    try {
      fileWatchService = FileSystems.getDefault().newWatchService();
      episodesDir.register(
          fileWatchService,
          StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_DELETE,
          StandardWatchEventKinds.ENTRY_MODIFY);

      running = true;
      watchThread = new Thread(this::watchLoop, "rss-watch");
      watchThread.setDaemon(true);
      watchThread.start();
      logger.info("Started watching: {}", episodesDir);
    } catch (IOException e) {
      logger.error("Failed to create file watcher for: {}", episodesDir, e);
      throw new RuntimeException("Failed to start file watcher", e);
    }
  }

  private void watchLoop() {
    while (running) {
      try {
        WatchKey key = fileWatchService.take();
        for (WatchEvent<?> event : key.pollEvents()) {
          if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
            continue;
          }

          Path filename = (Path) event.context();
          if (!isEpisodeFile(filename)) {
            continue;
          }

          logger.info("File change detected: {} ({})", filename, event.kind());
          newFileChanges.set(true);
        }
        key.reset();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        logger.error("Watch loop error: {}", e.getMessage(), e);
        break;
      }
    }
  }

  boolean isEpisodeFile(Path filename) {
    String name = filename.toString().toLowerCase();
    String ext = rssConfig.getEpisodeFileExtension().toLowerCase();
    return ext.isEmpty() || name.endsWith(ext);
  }

  @PreDestroy
  void stopWatching() {
    running = false;
    if (fileWatchService != null) {
      try {
        fileWatchService.close();
      } catch (IOException e) {
        logger.warn("Failed to close file watcher", e);
      }
    }
    if (watchThread != null) {
      watchThread.interrupt();
      try {
        watchThread.join(3000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    logger.info("Stopped watching");
  }

  @Scheduled(cron = "0 * * * * *")
  public void scheduledParseCheck() {
          if (newFileChanges.get() || (Boolean.TRUE.equals(rssConfig.getRunOnStartup()) && !successfulParse.get())) {
              try {
                  logger.info("New file changes found. Kicking off parse and write");
                  Show show = parseService.generateShow();
                  String path = rssService.writeRss(show);
                  logger.info("RSS feed written to: {}", path);
                  newFileChanges.set(false);
                  successfulParse.set(true);
              } catch (Exception e) {
                  logger.error("Failed to generate RSS feed: {}", e.getMessage(), e);
                  failureCounter.incrementAndGet();
                  if (failureCounter.get() > rssConfig.getFailureLimit()) {
                      newFileChanges.set(false);
                      logger.error(
                              "Reached RSS feed generation failure limit of {}. Ending retries.",
                              rssConfig.getFailureLimit());
                  }
              }
          } else {
              logger.info("No new changes found to files. Napping for a minute!");
          }
      }
}
