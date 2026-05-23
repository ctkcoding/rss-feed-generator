package com.ctkcoding.rssgen.service;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.model.Episode;
import com.ctkcoding.rssgen.model.Show;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEnclosureImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.feed.atom.Link;
import com.rometools.rome.io.SyndFeedOutput;
import com.rometools.modules.itunes.FeedInformation;
import com.rometools.modules.itunes.FeedInformationImpl;
import com.rometools.modules.itunes.EntryInformation;
import com.rometools.modules.itunes.EntryInformationImpl;
import com.rometools.modules.itunes.types.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class RssService {

    private static final Logger logger = LoggerFactory.getLogger(RssService.class);

    private final RssConfig rssConfig;

    public RssService(RssConfig rssConfig) {
        this.rssConfig = rssConfig;
      }

    // todo - write file properly
    public String writeRss(Show show) {
        SyndFeed feed = buildSyndFeed(show);
        String rssFileName = rssConfig.getRssFileName();
        Path outputPath = Path.of(System.getProperty("user.dir")).resolve(rssFileName);

        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
              }
            // todo - change
            File outputFile = outputPath.toFile();
            try (FileOutputStream os = new FileOutputStream(outputFile)) {
                SyndFeedOutput output = new SyndFeedOutput();
                output.output(feed, os);
              }
            logger.info("Wrote RSS feed to {}", outputPath);
            return outputPath.toString();
           } catch (IOException e) {
            logger.error("Failed to write RSS feed to {}: {}", outputPath, e.getMessage(), e);
            throw new RuntimeException("Failed to write RSS feed", e);
           }
        }

    private SyndFeed buildSyndFeed(Show show) {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        feed.setTitle(show.getTitle());
        feed.setDescription(show.getDescription());
        feed.setLink(show.getLink());
        feed.setLanguage(show.getLanguage());

        feed.setLink(show.getLink());

        FeedInformation channelItunes = new FeedInformationImpl();
        channelItunes.setAuthor(show.getDescription());
        channelItunes.setSubtitle(show.getTitle());
        channelItunes.setSummary(show.getDescription());
        channelItunes.setType("episodic");
        String imageUrl = show.getImage();
        if (imageUrl != null && !imageUrl.isBlank()) {
            channelItunes.setImageUri(imageUrl);
          }

        // todo -
        feed.setModules(List.of(channelItunes));

        List<SyndEntry> syndEntries = new ArrayList<>();
        if (show.getEpisodes() != null) {
            for (Episode episode : show.getEpisodes()) {
                syndEntries.add(buildSyndItem(episode));
              }
          }
        feed.setEntries(syndEntries);

        return feed;
      }

    private SyndEntry buildSyndItem(Episode episode) {
        SyndEntry entry = new SyndEntryImpl();
        entry.setTitle(episode.getTitle());

        SyndContent description = new SyndContentImpl();
        description.setType("text/html");
        description.setValue(episode.getDescription());
        entry.setDescription(description);

        entry.setLink(episode.getUrl());

        if (episode.getPubDate() != null) {
            entry.setPublishedDate(
                Date.from(episode.getPubDate().atZone(ZoneId.systemDefault()).toInstant())
              );
          }

        SyndEnclosure enclosure = new SyndEnclosureImpl();
        enclosure.setUrl(episode.getEnclosureUrl());
        enclosure.setType(episode.getEnclosureType());
        if (episode.getEnclosureLength() != null) {
            enclosure.setLength(episode.getEnclosureLength());
          }
        entry.setEnclosures(List.of(enclosure));

        EntryInformation itunesItem = new EntryInformationImpl();
        itunesItem.setTitle(episode.getTitle());
        itunesItem.setSubtitle(episode.getTitle());
        itunesItem.setSummary(episode.getDescription());
        String imageData = episode.getImage();
        if (imageData != null && !imageData.isBlank()) {
            itunesItem.setImageUri(imageData);
          }
        Duration duration = new Duration();
        duration.setMilliseconds(0); // TODO: extract duration from MP3 metadata tags
        itunesItem.setDuration(duration);
        itunesItem.setExplicit(false);
        entry.setModules(List.of(itunesItem));

        return entry;
      }
}
