package com.ctkcoding.rssgen.service;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.handler.ErrorLogHandler;
import com.ctkcoding.rssgen.model.Episode;
import com.ctkcoding.rssgen.model.Show;
import com.rometools.modules.itunes.EntryInformation;
import com.rometools.modules.itunes.EntryInformationImpl;
import com.rometools.modules.itunes.FeedInformation;
import com.rometools.modules.itunes.FeedInformationImpl;
import com.rometools.modules.itunes.types.Duration;
import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Description;
import com.rometools.rome.feed.rss.Enclosure;
import com.rometools.rome.feed.rss.Item;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedOutput;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RssService {

  private static final Logger logger = LoggerFactory.getLogger(RssService.class);
  private static final ZoneId UTC_ZONE = ZoneId.of("UTC");

  private final RssConfig rssConfig;
  private final ErrorLogHandler errorLogHandler;

  RssService(RssConfig rssConfig, ErrorLogHandler errorLogHandler) {
    this.rssConfig = rssConfig;
    this.errorLogHandler = errorLogHandler;
  }

  public String writeRss(Show show) {
    Channel channel = buildChannel(show);

    String rssFileName = rssConfig.getRssFileName();
    Path outputPath =
        Path.of(System.getProperty("user.dir"))
            .resolve(rssConfig.getInfoDir())
            .resolve(rssFileName);

    try {
      Path parent = outputPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      File outputFile = outputPath.toFile();
      WireFeedOutput output = new WireFeedOutput();
      output.output(channel, outputFile);
      logger.info("Wrote RSS feed to {}", outputPath);
      return outputPath.toString();
    } catch (IOException | FeedException e) {
      errorLogHandler.writeError(
          ParseErrorReason.RSS_OUTPUT_WRITE_FAILED, rssConfig.getRssFileName(), e.getMessage());
      logger.error("Failed to write RSS feed to {}: {}", outputPath, e.getMessage(), e);
      throw new RuntimeException("Failed to write RSS feed", e);
    }
  }

  private Channel buildChannel(Show show) {
    Channel channel = new Channel("rss_2.0");
    channel.setTitle(show.getTitle());
    channel.setDescription(show.getDescription());
    channel.setLink(show.getLink());
    channel.setLanguage(show.getLanguage());

    channel.setDocs("https://help.apple.com/itc/podcasts_connect/");
    channel.setManagingEditor(show.getTitle());

    FeedInformation channelItunes = new FeedInformationImpl();
    channelItunes.setAuthor(show.getTitle());
    channelItunes.setSubtitle(show.getTitle());
    channelItunes.setSummary(show.getDescription());
    String imageUrl = show.getImage();
    if (imageUrl != null && !imageUrl.isBlank()) {
      channelItunes.setImageUri(imageUrl);
    }
    channelItunes.setType("episodic");
    channel.getModules().add(channelItunes);

    if (show.getEpisodes() != null) {
      List<Item> items = new ArrayList<>();
      for (Episode episode : show.getEpisodes()) {
        items.add(buildItem(episode));
      }
      channel.setItems(items);
    }

    return channel;
  }

  private Item buildItem(Episode episode) {
    Item item = new Item();
    item.setTitle(episode.getTitle());
    item.setLink(episode.getUrl());
    item.setAuthor(episode.getTitle());

    Description description = new Description();
    description.setType("text/html");
    description.setValue(episode.getDescription());
    item.setDescription(description);

    if (episode.getPubDate() != null) {
      item.setPubDate(Date.from(episode.getPubDate().atZone(UTC_ZONE).toInstant()));
    }

    Enclosure enclosure = new Enclosure();
    enclosure.setUrl(episode.getEnclosureUrl());
    enclosure.setType(episode.getEnclosureType());
    if (episode.getEnclosureLength() != null) {
      enclosure.setLength(episode.getEnclosureLength());
    }
    item.setEnclosures(List.of(enclosure));

    EntryInformation itunesItem = new EntryInformationImpl();
    itunesItem.setTitle(episode.getTitle());
    itunesItem.setSubtitle(episode.getTitle());
    itunesItem.setSummary(episode.getDescription());
    String imageData = episode.getImage();
    if (imageData != null && !imageData.isBlank()) {
      itunesItem.setImageUri(imageData);
    }
    Duration duration = new Duration(episode.getDuration() * 1000L);
    itunesItem.setDuration(duration);
    itunesItem.setExplicit(false);
    item.getModules().add(itunesItem);

    return item;
  }
}
