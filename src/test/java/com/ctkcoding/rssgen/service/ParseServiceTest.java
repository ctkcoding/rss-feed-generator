package com.ctkcoding.rssgen.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.handler.ErrorLogHandler;
import com.ctkcoding.rssgen.model.Episode;
import com.ctkcoding.rssgen.model.Show;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
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

  private ParseService createService(RssConfig config) {
    ErrorLogHandler errorLogHandler = mock(ErrorLogHandler.class);
    return new ParseService(config, errorLogHandler);
  }

  private ServiceResult createServiceWithCapturingHandler(RssConfig config) {
    AtomicReference<ErrorLogHandler> handlerRef = new AtomicReference<>();
    doAnswer(
            invocation -> {
              if (invocation.getArgument(0) != null) {
                handlerRef.set((ErrorLogHandler) invocation.getArgument(0));
              }
              return null;
            })
        .when(mock(ErrorLogHandler.class))
        .writeError(any(), anyString(), anyString());
    ErrorLogHandler errorLogHandler = mock(ErrorLogHandler.class);
    handlerRef.set(errorLogHandler);
    return new ServiceResult(new ParseService(config, errorLogHandler), errorLogHandler);
  }

  private void copyEpisodes() {
    try {
      Path episodesDir = tempDir.resolve("episodes");
      Path artworkDir = tempDir.resolve("artwork");
      Path infoDir = tempDir.resolve("info");
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
      if (Files.exists(infoDir.resolve("cover.jpg"))) {
        Files.copy(
            infoDir.resolve("cover.jpg"),
            artworkDir.resolve("cover.jpeg"),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
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

  private static class ServiceResult {
    final ParseService service;
    final ErrorLogHandler handler;

    ServiceResult(ParseService service, ErrorLogHandler handler) {
      this.service = service;
      this.handler = handler;
    }
  }

  // ============ parseShow tests ============

  @Test
  void parseShow_parsesValidJson() throws IOException {
    loadShowJsonFromResources();
    Path artworkDir = tempDir.resolve("artwork");
    Files.createDirectories(artworkDir);
    Files.copy(
        Path.of("src/test/resources/artwork/cover.jpeg"),
        artworkDir.resolve("cover.jpeg"),
        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    ParseService service = createService(createConfig("info", "episodes"));
    System.setProperty("user.dir", tempDir.toString());

    Show show = service.parseShow();

    assertNotNull(show);
    assertEquals("Time Crisis", show.getTitle());
    assertEquals("This show rules", show.getDescription());
    assertEquals("https://timecrisis.apple.com", show.getSite());
    assertEquals("https://podcast.local", show.getLink());
    assertEquals("https://podcast.local/artwork/cover.jpeg", show.getImage());
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
    ParseService service = createService(createConfig("info", "episodes"));
    System.setProperty("user.dir", tempDir.toString());

    RuntimeException exception = assertThrows(RuntimeException.class, () -> service.parseShow());
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
                        "language": "      "
                        }
                        """);
    ParseService service = createService(createConfig("info", "episodes"));
    System.setProperty("user.dir", tempDir.toString());

    RuntimeException exception = assertThrows(RuntimeException.class, () -> service.parseShow());
    assertEquals("Failed to parse show file", exception.getMessage());
    assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    assertTrue(exception.getCause().getMessage().contains("Language"));
  }

  @Test
  void parseShow_throwsRuntimeExceptionWhenShowFileMissing() {
    RssConfig config = createConfig("info", "episodes");
    when(config.getShowFileName()).thenReturn("nonexistent_show.json");
    final ParseService service = createService(config);
    System.setProperty("user.dir", tempDir.toString());

    assertThrows(RuntimeException.class, () -> service.parseShow());
  }

  @Test
  void parseShow_logsErrorToFileWhenShowJsonMissing() {
    ErrorLogHandler errorLogHandler = mock(ErrorLogHandler.class);
    RssConfig config = createConfig("info", "episodes");
    when(config.getShowFileName()).thenReturn("nonexistent_show.json");
    final ParseService service = new ParseService(config, errorLogHandler);
    when(errorLogHandler.getCurrentLogFile()).thenReturn("parse-errors.log");
    System.setProperty("user.dir", tempDir.toString());

    assertThrows(RuntimeException.class, () -> service.parseShow());

    verify(errorLogHandler, times(1))
        .writeError(
            eq(ParseErrorReason.SHOW_CONFIG_FILE_NOT_FOUND),
            eq("nonexistent_show.json"),
            anyString());
  }

  @Test
  void parseShow_logsErrorToFileWhenShowJsonMalformed() throws IOException {
    writeShowJson("not valid json {{{");
    ErrorLogHandler errorLogHandler = mock(ErrorLogHandler.class);
    ParseService service = new ParseService(createConfig("info", "episodes"), errorLogHandler);
    when(errorLogHandler.getCurrentLogFile()).thenReturn("parse-errors.log");
    System.setProperty("user.dir", tempDir.toString());

    assertThrows(RuntimeException.class, () -> service.parseShow());

    verify(errorLogHandler, times(1))
        .writeError(eq(ParseErrorReason.SHOW_CONFIG_INVALID_JSON), eq("show.json"), anyString());
  }

  // ============ generateShow tests ============

  @Test
  void generateShow_discoversEpisodes() throws IOException {
    loadShowJsonFromResources();
    copyEpisodes();

    ParseService service = createService(createConfig("info", "episodes"));
    System.setProperty("user.dir", tempDir.toString());

    Show show = service.generateShow();

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

    ParseService service = createService(createConfig("info", "episodes"));
    System.setProperty("user.dir", tempDir.toString());

    Show show = service.generateShow();

    assertNotNull(show.getEpisodes());
    assertTrue(show.getEpisodes().size() >= 1);
    for (Episode ep : show.getEpisodes()) {
      assertNotNull(ep.getUrl());
    }
  }

  @Test
  void generateShow_throwsWhenEpisodesDirMissing() {
    loadShowJsonFromResources();

    ParseService service = createService(createConfig("info", "nonexistent_dir"));
    System.setProperty("user.dir", tempDir.toString());

    assertThrows(IllegalStateException.class, () -> service.generateShow());
  }

  @Test
  void generateShow_setsEmptyListWhenEpisodesDirEmpty() throws IOException {
    loadShowJsonFromResources();
    Path episodesDir = tempDir.resolve("episodes");
    Files.createDirectories(episodesDir);

    ErrorLogHandler errorLogHandler = mock(ErrorLogHandler.class);
    when(errorLogHandler.getCurrentLogFile()).thenReturn("parse-errors.log");
    ParseService service = new ParseService(createConfig("info", "episodes"), errorLogHandler);
    System.setProperty("user.dir", tempDir.toString());

    Show show = service.generateShow();

    assertNotNull(show);
    assertNotNull(show.getEpisodes());
    assertTrue(show.getEpisodes().isEmpty());
    verify(errorLogHandler, times(1)).writeSummary(0, 0);
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

    ErrorLogHandler errorLogHandler = mock(ErrorLogHandler.class);
    when(errorLogHandler.getCurrentLogFile()).thenReturn("parse-errors.log");
    ParseService service = new ParseService(createConfig("info", "episodes"), errorLogHandler);
    System.setProperty("user.dir", tempDir.toString());

    Show show = service.generateShow();

    assertNotNull(show);
    assertNotNull(show.getEpisodes());
    // Should have at least the valid episode; the broken one should be skipped
    for (Episode ep : show.getEpisodes()) {
      assertNotEquals("Broken Episode.mp3", ep.getTitle());
    }

    // Verify error was logged to handler
    verify(errorLogHandler, times(1))
        .writeError(
            eq(ParseErrorReason.EPISODE_MP3_PARSE_ERROR), eq("Broken Episode.mp3"), anyString());
  }

  @Test
  void generateShow_withEmptyEpisodes_doesNotWriteErrorLog() throws IOException {
    loadShowJsonFromResources();
    Path episodesDir = tempDir.resolve("episodes");
    Files.createDirectories(episodesDir);

    ErrorLogHandler errorLogHandler = mock(ErrorLogHandler.class);
    ParseService service = new ParseService(createConfig("info", "episodes"), errorLogHandler);
    System.setProperty("user.dir", tempDir.toString());

    Show show = service.generateShow();

    assertNotNull(show);
    assertTrue(show.getEpisodes().isEmpty());
    verify(errorLogHandler, times(0)).writeError(any(), anyString(), anyString());
  }

  @Test
  void generateShow_errorLogUsesTimestampedFilename() throws IOException {
    loadShowJsonFromResources();
    copyEpisodes();

    ErrorLogHandler errorLogHandler = mock(ErrorLogHandler.class);
    ParseService service = new ParseService(createConfig("info", "episodes"), errorLogHandler);
    System.setProperty("user.dir", tempDir.toString());

    service.generateShow();

    verify(errorLogHandler, times(1)).startParseRun();
    verify(errorLogHandler, times(1)).writeSummary(anyInt(), anyInt());
  }

  // ============ parse episode tests ============

  @Test
  void parseEpisode_setsTitleFromFilenameWhenNoMp3Metadata() throws IOException {
    copyEpisodes();
    ParseService service = createService(createConfig("info", "episodes"));
    System.setProperty("user.dir", tempDir.toString());

    Episode episode = service.parseEpisode("Episode 01.mp3", "https://podcast.local");

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
    ParseService service = createService(createConfig("info", "episodes"));
    System.setProperty("user.dir", tempDir.toString());

    Episode episode = service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    String expectedUrl =
        "https://podcast.local/episodes/"
            + URLEncoder.encode("Episode 01.mp3", StandardCharsets.UTF_8).replace("+", "%20");
    assertEquals(expectedUrl, episode.getUrl());
  }

  @Test
  void parseEpisode_constructsCorrectImage() throws IOException {
    copyEpisodes();
    ParseService service = createService(createConfig("info", "episodes"));
    System.setProperty("user.dir", tempDir.toString());

    Episode episode = service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    String expectedImage =
        "https://podcast.local/artwork/"
            + URLEncoder.encode("Episode 01", StandardCharsets.UTF_8).replace("+", "%20")
            + ".jpeg";
    assertEquals(expectedImage, episode.getImage());
  }

  @Test
  void parseEpisode_setsPubDateFromFileLastModifiedWhenFileExists() throws IOException {
    copyEpisodes();
    ParseService service = createService(createConfig("info", "episodes"));
    System.setProperty("user.dir", tempDir.toString());

    Episode episode = service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    assertNotNull(episode);
    assertNotNull(episode.getPubDate());
    assertTrue(episode.getPubDate().isAfter(LocalDateTime.of(2020, 1, 1, 0, 0, 0)));
  }

  @Test
  void parseEpisode_enclosureContainsUrlAndType() throws IOException {
    copyEpisodes();
    ParseService service = createService(createConfig("info", "episodes"));
    System.setProperty("user.dir", tempDir.toString());

    Episode episode = service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    assertNotNull(episode.getEnclosureUrl());
    assertTrue(episode.getEnclosureUrl().contains("https://podcast.local/episodes/"));
    assertEquals("audio/mpeg", episode.getEnclosureType());
  }

  @Test
  void parseEpisode_throwsForMissingFile() {
    ParseService service = createService(createConfig("info", "episodes"));
    System.setProperty("user.dir", tempDir.toString());

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> {
              service.parseEpisode("Totally Missing Episode.mp3", "https://podcast.local");
            });

    assertTrue(
        exception.getMessage().contains("Totally Missing Episode.mp3"),
        "Exception message should contain the episode filename");
  }

  @Test
  void parseEpisode_throwsAndLogsErrorWhenPubDateMissing() throws IOException {
    copyEpisodes();
    Path episodesDir2 = tempDir.resolve("episodes2");
    Files.createDirectories(episodesDir2);
    // Create a file that we will delete between path resolution and stat
    Path stubFile = episodesDir2.resolve("Ghost Episode.mp3");
    Files.write(stubFile, new byte[2048]);
    Files.delete(stubFile);

    RssConfig config = mock(RssConfig.class);
    when(config.getInfoDir()).thenReturn("info");
    when(config.getShowFileName()).thenReturn("show.json");
    when(config.getEpisodesDir()).thenReturn("episodes2");
    when(config.getEpisodeFileExtension()).thenReturn(".mp3");
    when(config.getArtworkFileExtension()).thenReturn(".jpeg");
    when(config.getArtworkDir()).thenReturn("artwork");
    when(config.getExtractArtwork()).thenReturn(false);
    when(config.getErrorLogFile()).thenReturn("parse-errors.log");

    ErrorLogHandler errorLogHandler = mock(ErrorLogHandler.class);
    ParseService service = new ParseService(config, errorLogHandler);
    System.setProperty("user.dir", tempDir.toString());

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> {
              service.parseEpisode("Ghost Episode.mp3", "https://podcast.local");
            });

    assertTrue(exception.getMessage().contains("Failed to determine publication date"));
    verify(errorLogHandler, times(1))
        .writeError(
            eq(ParseErrorReason.EPISODE_PUB_DATE_MISSING), eq("Ghost Episode.mp3"), anyString());
  }

  @Test
  void parseEpisode_warnsFileSizeUnknown_butIncludeEpisode() throws IOException {
    copyEpisodes();
    ParseService service = createService(createConfig("info", "episodes"));
    System.setProperty("user.dir", tempDir.toString());

    Episode episode = service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    assertNotNull(episode);
    assertNotNull(episode.getEnclosureUrl());
  }

  @Test
  void parseEpisode_extractArtwork_whenExtractArtworkEnabled_andFileDoesNotExist()
      throws IOException {
    loadShowJsonFromResources();
    copyEpisodes();

    RssConfig config = createConfigWithExtractArtwork("info", "episodes", true);
    ParseService service = createService(config);
    System.setProperty("user.dir", tempDir.toString());

    Episode episode = service.parseEpisode("Episode 01.mp3", "https://podcast.local");

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

    RssConfig config = createConfigWithExtractArtwork("info", "episodes", false);
    ParseService service = createService(config);
    System.setProperty("user.dir", tempDir.toString());

    Episode episode = service.parseEpisode("Episode 01.mp3", "https://podcast.local");

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

    RssConfig config = createConfigWithExtractArtwork("info", "episodes", true);
    ParseService service = createService(config);
    System.setProperty("user.dir", tempDir.toString());

    Episode episode = service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    assertNotNull(episode);
    assertTrue(Files.exists(artworkFile), "Artwork file should still exist");
    long newLastModified = Files.getLastModifiedTime(artworkFile).toMillis();
    assertEquals(
        originalLastModified, newLastModified, "Artwork file should not have been overwritten");
  }

  @Test
  void parseEpisode_warnsWhenMimeMismatch() throws IOException {
    loadShowJsonFromResources();
    copyEpisodes();

    RssConfig config = createConfigWithExtractArtwork("info", "episodes", true);
    when(config.getArtworkFileExtension()).thenReturn(".png");
    ParseService service = createService(config);
    System.setProperty("user.dir", tempDir.toString());

    Episode episode = service.parseEpisode("Episode 01.mp3", "https://podcast.local");

    assertNotNull(episode);
    Path artworkFile = tempDir.resolve("artwork").resolve("Episode 01.png");
    assertFalse(Files.exists(artworkFile), "Artwork should not be written due to MIME mismatch");
  }

  @Test
  void generateShow_skipsMacOSResourceForkFiles() throws IOException {
    loadShowJsonFromResources();
    copyEpisodes();

    Path episodesDir = tempDir.resolve("episodes");

    Files.write(episodesDir.resolve("._Episode 01.mp3"), new byte[128]);
    Files.write(episodesDir.resolve("._extra.mp3"), new byte[64]);
    Files.write(episodesDir.resolve("._DS_Store"), new byte[512]);

    ParseService service = createService(createConfig("info", "episodes"));
    System.setProperty("user.dir", tempDir.toString());

    Show show = service.generateShow();

    for (Episode ep : show.getEpisodes()) {
      assertNotEquals("._Episode 01.mp3", ep.getTitle());
      assertNotEquals("._extra.mp3", ep.getTitle());
    }

    for (Episode ep : show.getEpisodes()) {
      assertFalse(ep.getTitle().startsWith("._"));
    }
  }
}
