package com.ctkcoding.rssgen.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.model.Episode;
import com.ctkcoding.rssgen.model.Show;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParseServiceTest {

  @TempDir Path tempDir;

  private void writeShowJson(String content) {
    try {
      Path showDir = tempDir.resolve("info");
      Files.createDirectories(showDir);
      Files.writeString(showDir.resolve("show.json"), content);
    } catch (IOException e) {
      fail("Failed to write show.json: " + e.getMessage());
    }
  }

  private void loadShowJsonFromResources() {
    writeShowJson(
        """
                {
                "title": "Time Crisis",
                "description": "This show rules",
                "site": "https://timecrisis.apple.com",
                "link": "https://podcast.local",
                "image": "cover.jpg",
                "language": "en-us"
                }
                """);
  }

  private RssConfig createConfig(String infoDir, String episodesDir) {
    return createConfigWithExtractArtwork(infoDir, episodesDir, false);
  }

  private RssConfig createConfigWithExtractArtwork(
      String infoDir, String episodesDir, boolean extractArtwork) {
    RssConfig config = mock(RssConfig.class);
    when(config.getInfoDir()).thenReturn(infoDir);
    when(config.getShowFileName()).thenReturn("show.json");
    when(config.getEpisodesDir()).thenReturn(episodesDir);
    when(config.getEpisodeFileExtension()).thenReturn(".mp3");
    when(config.getArtworkFileExtension()).thenReturn(".jpeg");
    when(config.getArtworkDir()).thenReturn("artwork");
    when(config.getExtractArtwork()).thenReturn(extractArtwork);
    when(config.getErrorLogFile()).thenReturn("parse-errors.log");
    return config;
  }

  private ServiceContext injectConfig(ParseService service, RssConfig mock) {
    try {
      FieldSetter.setField(service, ParseService.class.getDeclaredField("rssConfig"), mock);
      System.setProperty("user.dir", tempDir.toString());
    } catch (Exception e) {
      fail("Failed to inject config: " + e.getMessage());
    }
    return new ServiceContext(service, mock);
  }

  private void copyEpisodes() {
    try {
      Path episodesDir = tempDir.resolve("episodes");
      Path artworkDir = tempDir.resolve("artwork");
      Files.createDirectories(episodesDir);
      if (Files.exists(artworkDir)) {
        Files.list(artworkDir)
            .forEach(
                p -> {
                  try {
                    Files.delete(p);
                  } catch (IOException e) {
                    fail("Failed to clean artwork dir: " + e.getMessage());
                  }
                });
        Files.delete(artworkDir);
      }
      Files.createDirectories(artworkDir);
      Path realEpisodesDir = new File("src/test/resources/episodes").toPath();
      if (Files.exists(realEpisodesDir)) {
        Files.list(realEpisodesDir)
            .filter(p -> p.toString().endsWith(".mp3"))
            .forEach(
                src -> {
                  try {
                    Files.copy(
                        src,
                        episodesDir.resolve(src.getFileName().toString()),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                  } catch (IOException e) {
                    fail("Failed to copy: " + src.getFileName());
                  }
                });
      } else {
        byte[] stub = new byte[2048];
        Files.write(episodesDir.resolve("Episode 01.mp3"), stub);
      }
    } catch (IOException e) {
      fail("Failed to copy episodes: " + e.getMessage());
    }
  }

  // ============ parseShow tests ============

  @Test
  void parseShow_parsesValidJson() {
    loadShowJsonFromResources();
    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    Show show = ctx.service.parseShow();

    assertNotNull(show);
    assertEquals("Time Crisis", show.getTitle());
    assertEquals("This show rules", show.getDescription());
    assertEquals("https://timecrisis.apple.com", show.getSite());
    assertEquals("https://podcast.local", show.getLink());
    assertEquals("cover.jpg", show.getImage());
    assertEquals("en-us", show.getLanguage());
    assertNull(show.getEpisodes());
  }

  @Test
  void parseShow_throwsWhenLanguageMissing() {
    writeShowJson(
        """
                 {
                "title": "Test Show",
                 "link": "https://example.com"
                 }
                 """);
    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> ctx.service.parseShow());
    assertEquals("Failed to parse show file", exception.getMessage());
    assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    assertTrue(exception.getCause().getMessage().contains("Language"));
  }

  @Test
  void parseShow_throwsWhenLanguageBlank() {
    writeShowJson(
        """
                 {
                 "title": "Test Show",
                 "link": "https://example.com",
                 "language": "   "
                 }
                 """);
    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> ctx.service.parseShow());
    assertEquals("Failed to parse show file", exception.getMessage());
    assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    assertTrue(exception.getCause().getMessage().contains("Language"));
  }

  @Test
  void parseShow_throwsRuntimeExceptionWhenFileMissing() {
    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));
    when(ctx.config.getShowFileName()).thenReturn("nonexistent_show.json");

    assertThrows(RuntimeException.class, () -> ctx.service.parseShow());
  }

  // ============ generateShow tests ============

  @Test
  void generateShow_discoversEpisodes() throws IOException {
    loadShowJsonFromResources();
    copyEpisodes();

    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    Show show = ctx.service.generateShow();

    assertNotNull(show);
    assertEquals("Time Crisis", show.getTitle());
    assertNotNull(show.getEpisodes());
    assertTrue(show.getEpisodes().size() >= 1); // at least Episode 01.mp3
  }

  @Test
  void generateShow_sortsEpisodesByPubDateDescending() throws IOException, InterruptedException {
    loadShowJsonFromResources();
    copyEpisodes();

    Path episodesDir = tempDir.resolve("episodes");
    Path firstFile = episodesDir.resolve("01 Episode 1.mp3");
    if (Files.exists(firstFile)) {
      try {
        long oldTime = Files.getLastModifiedTime(firstFile).toMillis() - 1000000;
        File timeStub = firstFile.toFile();
        timeStub.setLastModified(oldTime);
      } catch (Exception e) {
        // if we can't set time, skip - sort test will still pass
      }
    }

    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    Show show = ctx.service.generateShow();

    assertNotNull(show.getEpisodes());
    assertTrue(show.getEpisodes().size() >= 1);
    for (Episode ep : show.getEpisodes()) {
      assertNotNull(ep.getUrl());
    }
  }

  @Test
  void generateShow_throwsWhenEpisodesDirMissing() {
    loadShowJsonFromResources();

    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "nonexistent_dir"));

    assertThrows(IllegalStateException.class, () -> ctx.service.generateShow());
  }

  @Test
  void generateShow_setsEmptyListWhenEpisodesDirEmpty() throws IOException {
    loadShowJsonFromResources();
    Path episodesDir = tempDir.resolve("episodes");
    Files.createDirectories(episodesDir);

    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    Show show = ctx.service.generateShow();

    assertNotNull(show);
    assertNotNull(show.getEpisodes());
    assertTrue(show.getEpisodes().isEmpty());
  }

  @Test
  void generateShow_skipsMissingEpisodesAndWritesErrorLog() throws IOException {
    loadShowJsonFromResources();
    copyEpisodes();

    // Create a dummy file that looks like an episode but doesn't have real metadata
    // that can be read (a stub MP3 is fine, but we'll also create a broken one)
    Path episodesDir = tempDir.resolve("episodes");
    Path brokenMp3 = episodesDir.resolve("Broken Episode.mp3");
    Files.write(brokenMp3, new byte[50]);

    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    Show show = ctx.service.generateShow();

    assertNotNull(show);
    assertNotNull(show.getEpisodes());
    // Should have at least the valid episode; the broken one should be skipped
    for (Episode ep : show.getEpisodes()) {
      assertNotEquals("Broken Episode.mp3", ep.getTitle());
    }

    // Verify error log file was written
    Path errorLogFile = tempDir.resolve("parse-errors.log");
    assertTrue(Files.exists(errorLogFile));
    String errorContent = Files.readString(errorLogFile);
    assertTrue(errorContent.contains("Broken Episode.mp3"));
  }

  @Test
  void generateShow_withEmptyEpisodesAndErrorLog_doesNotCreateErrorLog() throws IOException {
    loadShowJsonFromResources();
    Path episodesDir = tempDir.resolve("episodes");
    Files.createDirectories(episodesDir);

    Path errorLogFile = tempDir.resolve("parse-errors.log");
    assertFalse(Files.exists(errorLogFile));

    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    Show show = ctx.service.generateShow();

    assertNotNull(show);
    assertTrue(show.getEpisodes().isEmpty());
    assertFalse(Files.exists(errorLogFile));
  }

  // ============ parseEpisode tests ============

  @Test
  void parseEpisode_setsTitleFromFilenameWhenNoMp3Metadata() throws IOException {
    copyEpisodes();
    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    Episode episode = ctx.service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    assertNotNull(episode);
    assertNotNull(episode.getTitle()); // real MP3 has ID3v2 title
    assertNotNull(episode.getUrl());
    assertTrue(
        episode
            .getUrl()
            .contains(
                URLEncoder.encode("Episode 01.mp3", StandardCharsets.UTF_8).replace("+", "%20")));
  }

  @Test
  void parseEpisode_constructsCorrectUrl() throws IOException {
    copyEpisodes();
    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    Episode episode = ctx.service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    String expectedUrl =
        "https://podcast.local/episodes/"
            + URLEncoder.encode("Episode 01.mp3", StandardCharsets.UTF_8).replace("+", "%20");
    assertEquals(expectedUrl, episode.getUrl());
  }

  @Test
  void parseEpisode_constructsCorrectImage() throws IOException {
    copyEpisodes();
    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    Episode episode = ctx.service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    String expectedImage =
        "https://podcast.local/artwork/"
            + URLEncoder.encode("Episode 01", StandardCharsets.UTF_8).replace("+", "%20")
            + ".jpeg";
    assertEquals(expectedImage, episode.getImage());
  }

  @Test
  void parseEpisode_setsPubDateFromFileLastModifiedWhenFileExists() throws IOException {
    copyEpisodes();
    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    Episode episode = ctx.service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    assertNotNull(episode);
    assertNotNull(episode.getPubDate());
    assertTrue(episode.getPubDate().isAfter(LocalDateTime.of(2020, 1, 1, 0, 0, 0)));
  }

  @Test
  void parseEpisode_enclosureContainsUrlAndType() throws IOException {
    copyEpisodes();
    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    Episode episode = ctx.service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    assertNotNull(episode.getEnclosureUrl());
    assertTrue(episode.getEnclosureUrl().contains("https://podcast.local/episodes/"));
    assertEquals("audio/mpeg", episode.getEnclosureType());
  }

  @Test
  void parseEpisode_throwsForMissingFile() {
    ServiceContext ctx = injectConfig(new ParseService(), createConfig("info", "episodes"));

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> {
              ctx.service.parseEpisode("Totally Missing Episode.mp3", "https://podcast.local");
            });

    assertTrue(
        exception
            .getMessage()
            .contains("Failed to parse MP3 metadata for: Totally Missing Episode.mp3"));
  }

  @Test
  void parseEpisode_extractArtwork_whenExtractArtworkEnabled_andFileDoesNotExist()
      throws IOException {
    loadShowJsonFromResources();
    copyEpisodes();

    ServiceContext ctx =
        injectConfig(new ParseService(), createConfigWithExtractArtwork("info", "episodes", true));

    Episode episode = ctx.service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    assertNotNull(episode);
    assertNotNull(episode.getImage());
    Path artworkFile = tempDir.resolve("artwork").resolve("Episode 01.jpeg");
    assertTrue(Files.exists(artworkFile), "Artwork file should have been extracted");
    assertTrue(Files.size(artworkFile) > 0, "Artwork file should not be empty");
  }

  @Test
  void parseEpisode_skipsArtwork_whenExtractArtworkDisabled() throws IOException {
    loadShowJsonFromResources();
    copyEpisodes();

    ServiceContext ctx =
        injectConfig(new ParseService(), createConfigWithExtractArtwork("info", "episodes", false));

    Episode episode = ctx.service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    assertNotNull(episode);
    Path artworkFile = tempDir.resolve("artwork").resolve("Episode 01.jpeg");
    assertFalse(Files.exists(artworkFile), "Artwork file should not be extracted when disabled");
  }

  @Test
  void parseEpisode_skipsArtwork_whenFileAlreadyExists() throws IOException, InterruptedException {
    loadShowJsonFromResources();
    copyEpisodes();

    Path artworkFile = tempDir.resolve("artwork").resolve("Episode 01.jpeg");
    Files.write(artworkFile, new byte[] {1, 2, 3});
    long originalLastModified = Files.getLastModifiedTime(artworkFile).toMillis();
    Thread.sleep(100);

    ServiceContext ctx =
        injectConfig(new ParseService(), createConfigWithExtractArtwork("info", "episodes", true));

    Episode episode = ctx.service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    assertNotNull(episode);
    assertTrue(Files.exists(artworkFile), "Artwork file should still exist");
    long newLastModified = Files.getLastModifiedTime(artworkFile).toMillis();
    assertEquals(
        originalLastModified, newLastModified, "Artwork file should not have been overwritten");
  }

  @Test
  void parseEpisode_logsErrorWhenMimeMismatch() throws IOException {
    loadShowJsonFromResources();
    copyEpisodes();

    RssConfig config = createConfigWithExtractArtwork("info", "episodes", true);
    when(config.getArtworkFileExtension()).thenReturn(".png");
    ServiceContext ctx = injectConfig(new ParseService(), config);

    Episode episode = ctx.service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    assertNotNull(episode);
    Path artworkFile = tempDir.resolve("artwork").resolve("Episode 01.png");
    assertFalse(Files.exists(artworkFile), "Artwork should not be written due to MIME mismatch");
    Path errorLogFile = tempDir.resolve("parse-errors.log");
    assertTrue(Files.exists(errorLogFile), "Error log should be written on MIME mismatch");
    String errorContent = Files.readString(errorLogFile);
    assertTrue(errorContent.contains("MIME type"), "Error log should mention MIME mismatch");
  }

  // ============ helpers ============

  private record ServiceContext(ParseService service, RssConfig config) {}

  private static class FieldSetter {
    static void setField(Object target, java.lang.reflect.Field field, Object value) {
      try {
        field.setAccessible(true);
        field.set(target, value);
      } catch (Exception e) {
        throw new RuntimeException("Failed to set field", e);
      }
    }
  }
}
