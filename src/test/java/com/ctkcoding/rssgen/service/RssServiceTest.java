package com.ctkcoding.rssgen.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.model.Episode;
import com.ctkcoding.rssgen.model.Show;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

public class RssServiceTest {

  private RssConfig rssConfig;
  private RssService rssService;
  private ErrorLogHandler mockErrorLogHandler;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    rssConfig =
        new RssConfig(
            null,
            null,
            null,
            "rss.xml",
            "show.json",
            ".mp3",
            ".jpg",
            "en",
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            5,
            "parse-errors.log");
    mockErrorLogHandler = Mockito.mock(ErrorLogHandler.class);
    Mockito.doNothing().when(mockErrorLogHandler).writeError(any(), anyString(), anyString());
    Mockito.doNothing().when(mockErrorLogHandler).writeSummary(anyInt(), anyInt());
    rssService = new RssService(rssConfig, mockErrorLogHandler);
  }

  private RssService createService(RssConfig config) {
    ErrorLogHandler handler = Mockito.mock(ErrorLogHandler.class);
    Mockito.doNothing().when(handler).writeError(any(), anyString(), anyString());
    Mockito.doNothing().when(handler).writeSummary(anyInt(), anyInt());
    return new RssService(config, handler);
  }

  private Show createTestShow() {
    Episode ep1 =
        Episode.builder()
            .title("Episode 1 - First Show")
            .description("This is the first episode.")
            .url("https://example.com/episodes/ep01.mp3")
            .pubDate(LocalDateTime.of(2025, 1, 15, 10, 0, 0))
            .image("https://example.com/artwork/ep01.jpg")
            .enclosureUrl("https://example.com/episodes/ep01.mp3")
            .enclosureType("audio/mpeg")
            .enclosureLength(5242880L)
            .build();

    Episode ep2 =
        Episode.builder()
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

  private String readXml(String filePath) throws IOException {
    return Files.readString(Path.of(filePath));
  }

  private String getTagValue(String xml, String tagName) {
    Pattern pattern =
        Pattern.compile(
            "<" + Pattern.quote(tagName) + ">\\s*([^<]*)\\s*</" + Pattern.quote(tagName) + ">",
            Pattern.DOTALL);
    Matcher matcher = pattern.matcher(xml);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return null;
  }

  private String getTagAttribute(String xml, String tagName, String attributeName) {
    Pattern pattern =
        Pattern.compile(
            "<"
                + Pattern.quote(tagName)
                + "[^>]*"
                + Pattern.quote(attributeName)
                + "\\s*=\\s*\"([^\"]*)\"",
            Pattern.DOTALL);
    Matcher matcher = pattern.matcher(xml);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  private boolean tagExists(String xml, String tagName) {
    return xml.contains(tagName);
  }

  @Test
  void writeRss_generatesFile() throws IOException {
    String filePath = tempDir.resolve("rss.xml").toString();
    rssConfig =
        new RssConfig(
            null,
            null,
            null,
            filePath,
            "show.json",
            ".mp3",
            ".jpg",
            "en",
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            5,
            "parse-errors.log");
    rssService = createService(rssConfig);

    Show show = createTestShow();
    String resultPath = rssService.writeRss(show);

    assertTrue(resultPath.contains("rss.xml"));
    File rssFile = new File(resultPath);
    assertTrue(rssFile.exists(), "RSS file should exist");
  }

  @Test
  void writeRss_channelFields() throws IOException {
    String filePath = tempDir.resolve("channel.xml").toString();
    rssConfig =
        new RssConfig(
            null,
            null,
            null,
            filePath,
            "show.json",
            ".mp3",
            ".jpg",
            "en",
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            5,
            "parse-errors.log");
    rssService = createService(rssConfig);

    Show show = createTestShow();
    rssService.writeRss(show);

    String xml = readXml(filePath);
    System.out.println("=== CHANNEL XML ===");
    System.out.println(xml);
    System.out.println("===================");
    assertEquals("My Awesome Podcast", getTagValue(xml, "title"));
    assertEquals("A fantastic podcast about everything.", getTagValue(xml, "description"));
    assertEquals("https://example.com", getTagValue(xml, "link"));
    assertEquals("en-us", getTagValue(xml, "language"));
    assertTrue(tagExists(xml, "<rss"));
    assertTrue(xml.contains("version=\"2.0\""));
  }

  @Test
  void writeRss_itemFields() throws IOException {
    String filePath = tempDir.resolve("items.xml").toString();
    rssConfig =
        new RssConfig(
            null,
            null,
            null,
            filePath,
            "show.json",
            ".mp3",
            ".jpg",
            "en",
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            5,
            "parse-errors.log");
    rssService = createService(rssConfig);

    Show show = createTestShow();
    rssService.writeRss(show);

    String xml = readXml(filePath);

    // Find content between <item> and </item>
    Pattern itemPattern = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL);
    Matcher itemMatcher = itemPattern.matcher(xml);

    List<String> items = new java.util.ArrayList<>();
    while (itemMatcher.find()) {
      items.add(itemMatcher.group(1));
    }
    assertEquals(2, items.size());

    String firstItem = items.get(0);
    assertEquals("Episode 1 - First Show", getTagValue(firstItem, "title"));
    assertEquals("This is the first episode.", getTagValue(firstItem, "description"));
    assertEquals("https://example.com/episodes/ep01.mp3", getTagValue(firstItem, "link"));
    assertEquals(
        "https://example.com/episodes/ep01.mp3", getTagAttribute(firstItem, "enclosure", "url"));
    assertEquals("audio/mpeg", getTagAttribute(firstItem, "enclosure", "type"));
    assertEquals("5242880", getTagAttribute(firstItem, "enclosure", "length"));
    assertEquals("Wed, 15 Jan 2025 10:00:00 GMT", getTagValue(firstItem, "pubDate"));
  }

  @Test
  void writeRss_itunesChannelModule() throws IOException {
    String filePath = tempDir.resolve("itunes_channel.xml").toString();
    rssConfig =
        new RssConfig(
            null,
            null,
            null,
            filePath,
            "show.json",
            ".mp3",
            ".jpg",
            "en",
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            5,
            "parse-errors.log");
    rssService = createService(rssConfig);

    Show show = createTestShow();
    rssService.writeRss(show);

    String xml = readXml(filePath);

    // Verify iTunes namespace declaration
    assertTrue(tagExists(xml, "xmlns:itunes="));

    // Verify iTunes channel module values
    String itunesSection = extractBetween(xml, "<channel>", "</channel>");
    assertEquals("My Awesome Podcast", getTagValue(itunesSection, "itunes:author"));
    assertEquals("My Awesome Podcast", getTagValue(itunesSection, "itunes:subtitle"));
    assertEquals(
        "A fantastic podcast about everything.", getTagValue(itunesSection, "itunes:summary"));
    assertEquals(
        "https://example.com/podcast-cover.jpg",
        getTagAttribute(itunesSection, "itunes:image", "href"));
    assertEquals("episodic", getTagValue(itunesSection, "itunes:type"));
  }

  @Test
  void writeRss_itunesItemModules() throws IOException {
    String filePath = tempDir.resolve("itunes_item.xml").toString();
    rssConfig =
        new RssConfig(
            null,
            null,
            null,
            filePath,
            "show.json",
            ".mp3",
            ".jpg",
            "en",
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            5,
            "parse-errors.log");
    rssService = createService(rssConfig);

    Show show = createTestShow();
    rssService.writeRss(show);

    String xml = readXml(filePath);
    Pattern itemPattern = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL);
    Matcher itemMatcher = itemPattern.matcher(xml);
    itemMatcher.find(); // first episode
    String firstItem = itemMatcher.group(1);

    assertEquals("Episode 1 - First Show", getTagValue(firstItem, "itunes:subtitle"));
    assertEquals("This is the first episode.", getTagValue(firstItem, "itunes:summary"));
    assertEquals(
        "https://example.com/artwork/ep01.jpg", getTagAttribute(firstItem, "itunes:image", "href"));
    assertEquals("00:00:00", getTagValue(firstItem, "itunes:duration"));
    assertTrue(tagExists(xml, "<itunes:explicit>no</itunes:explicit>"));
  }

  @Test
  void writeRss_emptyEpisodes_writesValidFeed() throws IOException {
    String filePath = tempDir.resolve("empty.xml").toString();
    rssConfig =
        new RssConfig(
            null,
            null,
            null,
            filePath,
            "show.json",
            ".mp3",
            ".jpg",
            "en",
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            5,
            "parse-errors.log");
    rssService = createService(rssConfig);

    Show show =
        Show.builder()
            .title("No Episodes Podcast")
            .description("A podcast with no episodes yet.")
            .link("https://example.com")
            .language("en")
            .episodes(List.of())
            .build();

    String result = rssService.writeRss(show);
    String xml = readXml(result);
    assertFalse(tagExists(xml, "<item"));
  }

  @Test
  void writeRss_cannotWrite_throwsRuntimeException() {
    String unwriteablePath = "/nonexistent/directory/that/cannot/be/accessed/rss.xml";
    rssConfig =
        new RssConfig(
            null,
            null,
            null,
            unwriteablePath,
            "show.json",
            ".mp3",
            ".jpg",
            "en",
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            5,
            "parse-errors.log");
    rssService = createService(rssConfig);

    Show show = createTestShow();

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> rssService.writeRss(show));
    assertTrue(exception.getMessage().contains("Failed to write RSS feed"));
  }

  @Test
  void writeRss_xmlStructure_validRss20() throws IOException {
    String filePath = tempDir.resolve("structure.xml").toString();
    rssConfig =
        new RssConfig(
            null,
            null,
            null,
            filePath,
            "show.json",
            ".mp3",
            ".jpg",
            "en",
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            Boolean.valueOf(false),
            5,
            "parse-errors.log");
    rssService = createService(rssConfig);

    Show show = createTestShow();
    rssService.writeRss(show);

    String xml = readXml(filePath);

    assertTrue(xml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
    assertTrue(xml.contains("<rss xmlns:itunes="));
    assertTrue(xml.contains("version=\"2.0\""));
    assertTrue(xml.contains("<channel>"));
    assertTrue(xml.contains("</channel>"));
    assertTrue(xml.contains("<item>"));
    assertTrue(xml.contains("</item>"));
  }

  private String extractBetween(String text, String start, String end) {
    int startIdx = text.indexOf(start);
    if (startIdx == -1) return "";
    int endIdx = text.indexOf(end, startIdx + start.length());
    if (endIdx == -1) return "";
    return text.substring(startIdx + start.length(), endIdx);
  }
}
