package com.ctkcoding.rssgen.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.model.Show;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatcherServiceTest {

  @Mock ParseService parseService;

  @Mock RssService rssService;

  @Mock RssConfig rssConfig;

  @InjectMocks WatcherService watcherService;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    lenient().when(rssConfig.getFailureLimit()).thenReturn(5);
    lenient().when(rssConfig.getFileWatch()).thenReturn(false);
    lenient().when(rssConfig.getEpisodesDir()).thenReturn("episodes");
  }

  // ============ Happy path: startWatching =============

  @Test
  void startWatching_fileWatchDisabled_doesNothing() {
    when(rssConfig.getFileWatch()).thenReturn(false);

    watcherService.startWatching();

    assertNull(watcherService.watchThread);
    assertNull(watcherService.fileWatchService);
  }

  @Test
  void startWatching_fileWatchEnabled_registersAndStarts() throws Exception {
    when(rssConfig.getFileWatch()).thenReturn(true);
    Path episodesPath = tempDir.resolve("episodes");
    Files.createDirectories(episodesPath);
    lenient().when(rssConfig.getEpisodesDir()).thenReturn("episodes");

    watcherService.startWatching();

    assertNotNull(watcherService.watchThread);
    assertTrue(watcherService.running);
    assertEquals("rss-watch", watcherService.watchThread.getName());
    try {
      assertTrue(watcherService.watchThread.isDaemon());
    } finally {
      watcherService.stopWatching();
    }
  }

  // ============ Happy path: isEpisodeFile =============

  @Test
  void isEpisodeFile_matchesEpisodeExtension() {
    when(rssConfig.getEpisodeFileExtension()).thenReturn(".mp3");

    assertTrue(watcherService.isEpisodeFile(Path.of("episode.mp3")));
  }

  @Test
  void isEpisodeFile_doesNotMatchDifferentExtension() {
    when(rssConfig.getEpisodeFileExtension()).thenReturn(".mp3");

    assertFalse(watcherService.isEpisodeFile(Path.of("document.txt")));
    assertFalse(watcherService.isEpisodeFile(Path.of("cover.jpg")));
  }

  @Test
  void isEpisodeFile_caseInsensitive() {
    when(rssConfig.getEpisodeFileExtension()).thenReturn(".mp3");

    assertTrue(watcherService.isEpisodeFile(Path.of("EPISODE.MP3")));
    assertTrue(watcherService.isEpisodeFile(Path.of("Episode.Mp3")));
  }

  @Test
  void isEpisodeFile_emptyExtensionMatchesAny() {
    when(rssConfig.getEpisodeFileExtension()).thenReturn("");

    assertTrue(watcherService.isEpisodeFile(Path.of("anything.txt")));
    assertTrue(watcherService.isEpisodeFile(Path.of("anything.mp3")));
  }

  // ============ Sad path: stopWatching ================

  @Test
  void stopWatching_nullThreadAndFileWatchService_doesNotNpe() throws Exception {
    Thread testThread =
        new Thread(
            () -> {
              try {
                Thread.sleep(5000);
              } catch (InterruptedException e) {
                // interrupted - threadFinished will be set by the interrupt
              }
            });
    testThread.start();
    watcherService.watchThread = testThread;
    watcherService.fileWatchService = null;

    watcherService.stopWatching();

    testThread.join(5000);
    assertFalse(watcherService.running);
  }

  // ============ Happy path: checkForNewChanges =========

  @Test
  void checkForNewChanges_noChanges_doesNotCallParseOrRss() {
    watcherService.newFileChanges.set(false);
    watcherService.checkForNewChanges();

    verifyNoInteractions(parseService);
    verifyNoInteractions(rssService);
    assertEquals(0, watcherService.failureCounter.get());
  }

  @Test
  void checkForNewChanges_withChanges_succeeds_resetsFlag() {
    Show show =
        Show.builder().title("Test Show").link("https://podcast.local").language("en-us").build();

    when(parseService.generateShow()).thenReturn(show);
    when(rssService.writeRss(show)).thenReturn("rss.xml");

    watcherService.newFileChanges.set(true);
    watcherService.checkForNewChanges();

    verify(parseService, times(1)).generateShow();
    verify(rssService, times(1)).writeRss(show);
    assertFalse(watcherService.newFileChanges.get());
    assertEquals(0, watcherService.failureCounter.get());
  }

  @Test
  void checkForNewChanges_rssFailure_incrementsCounter_keepsFlagTrue() {
    when(parseService.generateShow())
        .thenReturn(
            Show.builder()
                .title("Test Show")
                .link("https://podcast.local")
                .language("en-us")
                .build());
    when(rssService.writeRss(any())).thenThrow(new RuntimeException("Write failed"));

    watcherService.newFileChanges.set(true);
    watcherService.checkForNewChanges();

    assertEquals(1, watcherService.failureCounter.get());
    assertTrue(watcherService.newFileChanges.get());
  }

  @Test
  void checkForNewChanges_failureLimitExceeded_resetsFlag_noMoreRetries() {
    Show show =
        Show.builder().title("Test Show").link("https://podcast.local").language("en-us").build();
    when(parseService.generateShow()).thenReturn(show);
    when(rssService.writeRss(any())).thenThrow(new RuntimeException("Write failed"));

    watcherService.newFileChanges.set(true);
    watcherService.failureCounter.set(5);
    watcherService.checkForNewChanges();

    assertEquals(6, watcherService.failureCounter.get());
    assertFalse(watcherService.newFileChanges.get());
  }

  @Test
  void checkForNewChanges_counterEqualToLimit_doesNotStopRetries() {
    Show show =
        Show.builder().title("Test Show").link("https://podcast.local").language("en-us").build();
    when(parseService.generateShow()).thenReturn(show);
    when(rssService.writeRss(any())).thenThrow(new RuntimeException("Write failed"));

    watcherService.newFileChanges.set(true);
    watcherService.failureCounter.set(4);
    watcherService.checkForNewChanges();

    assertTrue(watcherService.newFileChanges.get());
    assertEquals(5, watcherService.failureCounter.get());
  }

  @Test
  void checkForNewChanges_failureThenSuccess_resetsFlag_keepsCounter() {
    Show show =
        Show.builder().title("Test Show").link("https://podcast.local").language("en-us").build();

    when(parseService.generateShow()).thenReturn(show);
    when(rssService.writeRss(any()))
        .thenThrow(new RuntimeException("Write failed"))
        .thenReturn("rss.xml");

    watcherService.newFileChanges.set(true);
    watcherService.checkForNewChanges();

    int counterAfterFailure = watcherService.failureCounter.get();
    assertEquals(1, counterAfterFailure);
    assertTrue(watcherService.newFileChanges.get());

    watcherService.newFileChanges.set(true);
    watcherService.checkForNewChanges();

    int counterAfterSuccess = watcherService.failureCounter.get();
    assertEquals(1, counterAfterSuccess);
    assertFalse(watcherService.newFileChanges.get());
  }

  // ============ Sad path: directory not found =========

  @Test
  void startWatching_directoryNotFound_throwsIllegalStateException() {
    when(rssConfig.getFileWatch()).thenReturn(true);
    when(rssConfig.getEpisodesDir()).thenReturn("nonexistent_dir_xyz");

    assertThrows(IllegalStateException.class, () -> watcherService.startWatching());
  }

  // ============ Integration: real filesystem events ========

  @Test
  void watchLoop_realFileEvent_setsNewFileChangesFlag() throws Exception {
    when(rssConfig.getFileWatch()).thenReturn(true);
    when(rssConfig.getEpisodeFileExtension()).thenReturn(".mp3");
    Path episodesPath = tempDir.resolve("episodes");
    Files.createDirectories(episodesPath);
    lenient().when(rssConfig.getEpisodesDir()).thenReturn("episodes");
    String originalUserDir = System.getProperty("user.dir");

    try {
      System.setProperty("user.dir", tempDir.toString());
      watcherService.startWatching();

      Files.createFile(episodesPath.resolve("new episode.mp3"));

      for (int i = 0; i < 20; i++) {
        if (watcherService.newFileChanges.get()) break;
        Thread.sleep(100);
      }

      assertTrue(
          watcherService.newFileChanges.get(), "Flag should be set after new episode file created");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      fail("Test interrupted");
    } finally {
      watcherService.stopWatching();
      System.setProperty("user.dir", originalUserDir);
    }
  }

  @Test
  void watchLoop_nonEpisodeFile_doesNotSetFlag() throws Exception {
    when(rssConfig.getFileWatch()).thenReturn(true);
    lenient().when(rssConfig.getEpisodeFileExtension()).thenReturn(".mp3");
    Path episodesPath = tempDir.resolve("episodes");
    Files.createDirectories(episodesPath);
    String originalUserDir = System.getProperty("user.dir");

    try {
      System.setProperty("user.dir", tempDir.toString());
      watcherService.startWatching();

      Files.createFile(episodesPath.resolve("document.txt"));
      Thread.sleep(1000);

      assertFalse(
          watcherService.newFileChanges.get(), "Flag should not be set for non-episode file");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      fail("Test interrupted");
    } finally {
      watcherService.stopWatching();
      System.setProperty("user.dir", originalUserDir);
    }
  }
}
