package com.ctkcoding.rssgen.handler;

import static com.ctkcoding.rssgen.utils.Constants.LOG_FILE_EXTENSION;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.service.ParseErrorReason;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ErrorLogHandler {

  private static final Logger logger = LoggerFactory.getLogger(ErrorLogHandler.class);
  private static final DateTimeFormatter FILE_TIME_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

  private final RssConfig rssConfig;
  private volatile String currentLogFile;

  ErrorLogHandler(RssConfig rssConfig) {
    this.rssConfig = rssConfig;
  }

  public String startParseRun() {
    String timestamp = LocalDateTime.now().format(FILE_TIME_FMT);
    currentLogFile = rssConfig.getErrorLogFilePrefix() + timestamp + LOG_FILE_EXTENSION;
    String basePath = System.getProperty("user.dir");
    Path errorLogDir = Path.of(basePath, rssConfig.getInfoDir());
    try {
      Files.createDirectories(errorLogDir);
    } catch (IOException e) {
      logger.error("Failed to create error log directory: {}", errorLogDir, e);
      return null;
    }
    Path errorLogFile = errorLogDir.resolve(currentLogFile);
    try {
      Files.writeString(
          errorLogFile,
          "=== Parse run started: " + timestamp + " ===" + System.lineSeparator(),
          java.nio.file.StandardOpenOption.CREATE,
          java.nio.file.StandardOpenOption.APPEND);
    } catch (IOException e) {
      logger.error("Failed to write error log header: {}", errorLogFile, e);
    }
    return currentLogFile;
  }

  public void writeError(ParseErrorReason reason, String context, String detail) {
    if (currentLogFile == null) return;
    String basePath = System.getProperty("user.dir");
    Path errorLogDir = Path.of(basePath, rssConfig.getInfoDir());
    Path errorLogFile = errorLogDir.resolve(currentLogFile);

    StringBuilder line = new StringBuilder();
    line.append(reason.isWarning() ? "[WARN] " : "[ERROR] ");
    line.append(reason.getLabel());
    if (context != null && !context.isBlank()) {
      line.append(" - ").append(context);
    }
    if (detail != null && !detail.isBlank()) {
      line.append(": ").append(detail);
    }

    try {
      Files.writeString(
          errorLogFile,
          line + System.lineSeparator(),
          java.nio.file.StandardOpenOption.CREATE,
          java.nio.file.StandardOpenOption.APPEND);
      logger.warn("Wrote [{}] error: {}", reason.name(), line);
    } catch (IOException e) {
      logger.error("Failed to write error log: {}", errorLogFile, e);
    }
  }

  public void writeSummary(int totalEpisodes, int failures) {
    if (currentLogFile == null) return;
    String basePath = System.getProperty("user.dir");
    Path errorLogDir = Path.of(basePath, rssConfig.getInfoDir());
    Path errorLogFile = errorLogDir.resolve(currentLogFile);
    if (!Files.exists(errorLogFile)) return;

    String summary = "Parsed " + totalEpisodes + " episodes, " + failures + " failures.";
    try {
      Files.writeString(
          errorLogFile,
          summary + System.lineSeparator(),
          java.nio.file.StandardOpenOption.CREATE,
          java.nio.file.StandardOpenOption.APPEND);
    } catch (IOException e) {
      logger.error("Failed to write summary line: {}", errorLogFile, e);
    }
  }

  public String getCurrentLogFile() {
    return currentLogFile;
  }
}
