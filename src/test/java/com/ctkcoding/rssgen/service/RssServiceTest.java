package com.ctkcoding.rssgen.service;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.model.Episode;
import com.ctkcoding.rssgen.model.Show;
import com.rometools.rome.feed.SyndFeed;
import com.rometools.rome.feed.SyndContent;
import com.rometools.rome.feed.atom.Link;
import com.rometools.rome.feed.module.itunes.impl.ItunesChannelModuleImpl;
import com.rometools.rome.feed.module.itunes.impl.ItunesItemModuleImpl;
import com.rometools.rome.feed.rss.Enclosure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RssServiceTest {

    private RssConfig rssConfig;
    private RssService rssService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        rssConfig = new RssConfig(
            null, null, null,
            "rss.xml", "show.json", ".mp3", ".jpg",
            "en",
            false,
            false
         );
        rssConfig.setErrorLogFile("parse-errors.log");
        rssService = new RssService(rssConfig);
     }

    private Show createTestShow() {
        Episode ep1 = Episode.builder()
            .title("Episode 1 - First Show")
            .description("This is the first episode.")
            .url("https://example.com/episodes/ep01.mp3")
            .pubDate(LocalDateTime.of(2025, 1, 15, 10, 0, 0))
            .image("https://example.com/artwork/ep01.jpg")
            .enclosureUrl("https://example.com/episodes/ep01.mp3")
            .enclosureType("audio/mpeg")
            .enclosureLength(5242880L)
            .build();

        Episode ep2 = Episode.builder()
            .title("Episode 2 - Second Show")
            .description("This is the second episode.")
            .url("https://example.com/episodes/ep02.mp3")
            .pubDate(LocalDateTime.of(2025, 2, 1, 8, 0, 0))
            .image("https://example.com/artwork/ep02.jpg")
            .enclosureUrl("https://example.com/episodes/ep02.mp3")
            .enclosureType("audio/mpeg")
            .enclosureLength(6291456L)
            .build();

        return Show.builder()
            .title("My Awesome Podcast")
            .description("A fantastic podcast about everything.")
            .site("https://example.com")
            .link("https://example.com")
            .image("https://example.com/podcast-cover.jpg")
            .language("en-us")
            .episodes(List.of(ep1, ep2))
            .build();
     }

    @Test
    void testWriteRss_generatesFile() {
        String filePath = tempDir.resolve("rss.xml").toString();
        rssConfig = new RssConfig(
            null, null, null, filePath, "show.json", ".mp3", ".jpg",
            "en", false, false
        );
        rssService = new RssService(rssConfig);

        Show show = createTestShow();
        String resultPath = rssService.writeRss(show);

        assertTrue(resultPath.contains("rss.xml"));
        File rssFile = new File(resultPath);
        assertTrue(rssFile.exists(), "RSS file should exist");
     }

    @Test
    void testWriteRss_channelFields() {
        String filePath = tempDir.resolve("channel.xml").toString();
        rssConfig = new RssConfig(
            null, null, null, filePath, "show.json", ".mp3", ".jpg",
            "en", false, false
        );
        rssService = new RssService(rssConfig);

        Show show = createTestShow();
        rssService.writeRss(show);

        SyndFeed feed = readFeed(filePath);
        assertEquals("My Awesome Podcast", feed.getTitle());
        assertEquals("A fantastic podcast about everything.", feed.getDescription());
        assertEquals("https://example.com", feed.getLink());
        assertEquals("en-us", feed.getLanguage());
     }

    @Test
    void testWriteRss_itemFields() {
        String filePath = tempDir.resolve("items.xml").toString();
        rssConfig = new RssConfig(
            null, null, null, filePath, "show.json", ".mp3", ".jpg",
            "en", false, false
        );
        rssService = new RssService(rssConfig);

        Show show = createTestShow();
        rssService.writeRss(show);

        SyndFeed feed = readFeed(filePath);
        List<?> items = feed.getItems();
        assertEquals(2, items.size());

        com.rometools.rome.feed.rss.Item firstItem = (com.rometools.rome.feed.rss.Item) items.get(0);
        assertEquals("Episode 1 - First Show", firstItem.getTitle());
        assertEquals("This is the first episode.", firstItem.getDescription().getValue());
        assertEquals("https://example.com/episodes/ep01.mp3", firstItem.getLink());

        Enclosure enc = firstItem.getEnclosure();
        assertEquals("https://example.com/episodes/ep01.mp3", enc.getUrl());
        assertEquals("audio/mpeg", enc.getType());
        assertEquals(5242880L, enc.getLength());
     }

    @Test
    void testWriteRss_itunesChannelModule() {
        String filePath = tempDir.resolve("itunes_channel.xml").toString();
        rssConfig = new RssConfig(
            null, null, null, filePath, "show.json", ".mp3", ".jpg",
            "en", false, false
        );
        rssService = new RssService(rssConfig);

        Show show = createTestShow();
        rssService.writeRss(show);

        SyndFeed feed = readFeed(filePath);
        com.rometools.rome.feed.module.Module channelModule = feed.getModule("http://.itunes.apple.com/");
        assertNotNull(channelModule, "iTunes channel module should be present");

        ItunesChannelModuleImpl itunes = (ItunesChannelModuleImpl) channelModule;
        assertEquals("A fantastic podcast about everything.", itunes.getAuthor());
        assertEquals("My Awesome Podcast", itunes.getSubtitle());
        assertEquals("A fantastic podcast about everything.", itunes.getSummary());
        assertEquals("https://example.com/podcast-cover.jpg", itunes.getImage());
        assertEquals("episodic", itunes.getType());
     }

    @Test
    void testWriteRss_itunesItemModules() {
        String filePath = tempDir.resolve("itunes_item.xml").toString();
        rssConfig = new RssConfig(
            null, null, null, filePath, "show.json", ".mp3", ".jpg",
            "en", false, false
        );
        rssService = new RssService(rssConfig);

        Show show = createTestShow();
        rssService.writeRss(show);

        SyndFeed feed = readFeed(filePath);
        com.rometools.rome.feed.rss.Item item = (com.rometools.rome.feed.rss.Item) feed.getItems().get(0);

        com.rometools.rome.feed.module.Module itunesModule = item.getModule("http://itun.es/modules/1.0/");
        assertNotNull(itunesModule, "iTunes item module should be present");

        ItunesItemModuleImpl itunes = (ItunesItemModuleImpl) itunesModule;
        assertEquals("Episode 1 - First Show", itunes.getSubtitle());
        assertEquals("This is the first episode.", itunes.getSummary());
        assertEquals("https://example.com/artwork/ep01.jpg", itunes.getEpisodeImage());
        assertEquals("0", itunes.getDuration());
        assertEquals("false", itunes.getExplicit());
        assertEquals("episodic", itunes.getType());
     }

    @Test
    void testWriteRss_atomSelfLink() {
        String filePath = tempDir.resolve("atom_link.xml").toString();
        rssConfig = new RssConfig(
            null, null, null, filePath, "show.json", ".mp3", ".jpg",
            "en", false, false
        );
        rssService = new RssService(rssConfig);

        Show show = createTestShow();
        rssService.writeRss(show);

        SyndFeed feed = readFeed(filePath);
        List<com.rometools.rome.feed.atom.Link> atomLinks = feed.getLinks();
        com.rometools.rome.feed.atom.Link selfLink = null;
        for (com.rometools.rome.feed.atom.Link link : atomLinks) {
            if ("self".equals(link.getRel())) {
                selfLink = link;
                break;
              }
          }
        assertNotNull(selfLink, "Self atom:link should be present");
        assertEquals("https://example.com", selfLink.getHref());
     }

    @Test
    void testWriteRss_emptyEpisodes() {
        String filePath = tempDir.resolve("empty.xml").toString();
        rssConfig = new RssConfig(
            null, null, null, filePath, "show.json", ".mp3", ".jpg",
            "en", false, false
        );
        rssService = new RssService(rssConfig);

        Show show = Show.builder()
            .title("No Episodes Podcast")
            .description("A podcast with no episodes yet.")
            .link("https://example.com")
            .language("en")
            .episodes(List.of())
            .build();

        String result = rssService.writeRss(show);
        SyndFeed feed = readFeed(result);
        assertTrue(feed.getItems().isEmpty());
     }

    private SyndFeed readFeed(String filePath) {
        try {
            com.rometools.rome.feed.synd.SyndFeedInput input = new com.rometools.rome.feed.synd.SyndFeedInput();
            return input.build(new java.io.FileReader(filePath));
         } catch (java.io.IOException | com.rometools.rome.feed.FeedException e) {
            throw new RuntimeException("Failed to parse generated RSS feed: " + e.getMessage(), e);
         }
     }
}
