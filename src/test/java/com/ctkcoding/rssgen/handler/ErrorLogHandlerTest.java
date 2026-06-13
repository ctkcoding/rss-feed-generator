package com.ctkcoding.rssgen.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.service.ParseErrorReason;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class ErrorLogHandlerTest {

  @TempDir Path tempDir;
  private String originalUserDir;

  @BeforeEach
  void setUp() {
    originalUserDir = System.getProperty("user.dir");
  }

  @AfterEach
  void tearDown() {
    System.setProperty("user.dir", originalUserDir);
  }

  private ErrorLogHandler createHandler(String errorLogFilePrefix, String artDir) {
    RssConfig config = Mockito.mock(RssConfig.class);
    when(config.getErrorLogFilePrefix()).thenReturn(errorLogFilePrefix);
    when(config.getArtworkDir()).thenReturn(artDir);
    when(config.getInfoDir()).thenReturn("info");
    System.setProperty("user.dir", tempDir.toString());
    return new ErrorLogHandler(config);
  }

  @Test
  void startParseRun_returnsTimestampedFilename() {
    ErrorLogHandler handler = createHandler("parse-errors-", "artwork");
    String filename = handler.startParseRun();

    assertNotNull(filename);
    assertTrue(filename.startsWith("parse-errors-"));
    assertTrue(filename.endsWith(".log"));
  }

  @Test
  void startParseRun_createsLogFile() throws IOException {
    ErrorLogHandler handler = createHandler("parse-errors-", "artwork");
    String filename = handler.startParseRun();
    Path logFile = tempDir.resolve("info").resolve(filename);

    // We can't resolve to tempDir directly since user.dir is not set,
    // so just verify the returned filename is non-null and matches pattern
    assertNotNull(filename);
    assertTrue(filename.startsWith("parse-errors-"));
    assertTrue(Files.exists(logFile), "Log file should exist at " + logFile);
  }

  @Test
  void writeError_appendsToCurrentLogFile() throws IOException {
    ErrorLogHandler handler = createHandler("parse-errors-", "artwork");
    handler.startParseRun();
    String filename = handler.getCurrentLogFile();

    handler.writeError(
        ParseErrorReason.EPISODE_MP3_PARSE_ERROR,
        "test.mp3",
        "InvalidDataException - File is not a valid MP3");

    Path logFile = tempDir.resolve("info").resolve(filename);
    assertTrue(Files.exists(logFile));
    String content = Files.readString(logFile);
    assertTrue(content.contains("[ERROR]"));
    assertTrue(content.contains("MP3 parse failed"));
    assertTrue(content.contains("test.mp3"));
    assertTrue(content.contains("InvalidDataException"));
  }

  @Test
  void writeError_warningUsesWarnPrefix() throws IOException {
    ErrorLogHandler handler = createHandler("parse-errors-", "artwork");
    handler.startParseRun();

    handler.writeError(ParseErrorReason.EPISODE_FILE_SIZE_UNKNOWN, "test.mp3", "No such file");

    Path logFile = tempDir.resolve("info").resolve(handler.getCurrentLogFile());
    String content = Files.readString(logFile);
    assertTrue(content.contains("[WARN]"), "Warning errors should use [WARN] prefix");
    assertFalse(content.contains("[ERROR]"), "Warning errors should not use [ERROR] prefix");
  }

  @Test
  void writeError_noopWithoutStartParseRun() {
    ErrorLogHandler handler = createHandler("parse-errors-", "artwork");
    // Should not throw NPE
    assertDoesNotThrow(
        () -> handler.writeError(ParseErrorReason.EPISODE_MP3_PARSE_ERROR, "test.mp3", "detail"));
  }

  @Test
  void writeSummary_appendsSummaryLine() throws IOException {
    ErrorLogHandler handler = createHandler("parse-errors-", "artwork");
    handler.startParseRun();

    handler.writeError(ParseErrorReason.EPISODE_MP3_PARSE_ERROR, "bad.mp3", "corrupt file");
    handler.writeSummary(5, 1);

    Path logFile = tempDir.resolve("info").resolve(handler.getCurrentLogFile());
    String content = Files.readString(logFile);
    assertTrue(content.contains("Parsed 5 episodes, 1 failures."));
  }

  @Test
  void writeSummary_noopWithoutStartParseRun() {
    ErrorLogHandler handler = createHandler("parse-errors-", "artwork");
    assertDoesNotThrow(() -> handler.writeSummary(0, 0));
  }

  @Test
  void writeSummary_noopWhenLogFileDoesNotExist() throws IOException {
    ErrorLogHandler handler = createHandler("parse-errors-", "artwork");
    // Don't call startParseRun, so no file exists
    assertDoesNotThrow(() -> handler.writeSummary(1, 0));
  }

  @Test
  void writeError_showsReasonLabel_andContext() throws IOException {
    ErrorLogHandler handler = createHandler("parse-errors-", "artwork");
    handler.startParseRun();

    handler.writeError(
        ParseErrorReason.SHOW_CONFIG_FILE_NOT_FOUND, "show.json", "No such file or directory");

    Path logFile = tempDir.resolve("info").resolve(handler.getCurrentLogFile());
    String content = Files.readString(logFile);
    assertTrue(content.contains("Show config - show.json not found"));
    assertTrue(content.contains("show.json"));
    assertTrue(content.contains("No such file or directory"));
  }

  @Test
  void writeError_showsReasonLabel_mimeMismatch() throws IOException {
    ErrorLogHandler handler = createHandler("parse-errors-", "artwork");
    handler.startParseRun();

    handler.writeError(
        ParseErrorReason.ARTWORK_MIME_MISMATCH,
        "ep01.mp3",
        "MIME type 'image/png' doesn't match configured extension '.jpeg'");

    Path logFile = tempDir.resolve("info").resolve(handler.getCurrentLogFile());
    String content = Files.readString(logFile);
    assertTrue(content.contains("WARNING: MIME type mismatch"));
    assertTrue(content.contains("ep01.mp3"));
  }

  @Test
  void startParseRun_headerWrittenToLogFile() throws IOException {
    ErrorLogHandler handler = createHandler("parse-errors-", "artwork");
    handler.startParseRun();

    Path logFile = tempDir.resolve("info").resolve(handler.getCurrentLogFile());
    String content = Files.readString(logFile);
    assertTrue(content.contains("=== Parse run started:"));
  }

  @Test
  void writeError_contextBlank_skipsDASH() throws IOException {
    ErrorLogHandler handler = createHandler("parse-errors-", "artwork");
    handler.startParseRun();

    handler.writeError(ParseErrorReason.EPISODE_PUB_DATE_MISSING, "", "file not found");

    Path logFile = tempDir.resolve("info").resolve(handler.getCurrentLogFile());
    String content = Files.readString(logFile);
    String[] lines = content.split(System.lineSeparator());
    String lastLine = lines[lines.length - 1];
    assertFalse(
        lastLine.contains(" - "), "Line should not have ' - ' separator when context is blank");
    assertTrue(
        lastLine.startsWith("[ERROR] Could not determine publication date: file not found"),
        "Direct concatenation without separator");
  }
}
