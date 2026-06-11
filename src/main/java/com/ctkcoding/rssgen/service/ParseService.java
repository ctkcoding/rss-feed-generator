package com.ctkcoding.rssgen.service;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.handler.ErrorLogHandler;
import com.ctkcoding.rssgen.model.Episode;
import com.ctkcoding.rssgen.model.Show;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ParseService {

  private static final Logger logger = LoggerFactory.getLogger(ParseService.class);

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final Map<String, String[]> MIME_TYPE_MAPPING =
      Map.of(
          "image/jpeg", new String[] {".jpg", ".jpeg"},
          "image/png", new String[] {".png"},
          "image/gif", new String[] {".gif"});

  private final RssConfig rssConfig;
  private final ErrorLogHandler errorLogHandler;

  @Autowired
  ParseService(RssConfig rssConfig, ErrorLogHandler errorLogHandler) {
    this.rssConfig = rssConfig;
    this.errorLogHandler = errorLogHandler;
  }

  public Show generateShow() {
    Show show;
    errorLogHandler.startParseRun();
    try {
      show = parseShow();
    } catch (Exception e) {
      String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      String showFileName = rssConfig.getShowFileName();
      errorLogHandler.writeError(
          e.getCause() instanceof java.io.FileNotFoundException
              ? ParseErrorReason.SHOW_CONFIG_FILE_NOT_FOUND
              : ParseErrorReason.SHOW_CONFIG_INVALID_JSON,
          showFileName,
          detail);
      logger.error("Failed to parse show file, aborting generation", e);
      throw new RuntimeException("Failed to parse show file", e);
    }

    String episodesDirPath = rssConfig.getEpisodesDir();
    Path episodesDir = Path.of(System.getProperty("user.dir")).resolve(episodesDirPath);

    if (!Files.exists(episodesDir) || !Files.isDirectory(episodesDir)) {
      throw new IllegalStateException("Episodes directory does not exist: " + episodesDirPath);
    }

    List<File> mp3Files;
    try {
      mp3Files =
          Files.walk(episodesDir)
              .filter(
                  p ->
                      p.toFile().isFile()
                          && p.toString().endsWith(rssConfig.getEpisodeFileExtension()))
              .map(Path::toFile)
              .sorted(Comparator.comparing(File::lastModified).reversed())
              .toList();
    } catch (IOException e) {
      logger.error("Failed to list episodes directory: {}", episodesDirPath, e);
      throw new IllegalStateException("Failed to list episodes directory: " + episodesDirPath, e);
    }

    if (mp3Files.isEmpty()) {
      show = show.toBuilder().episodes(new java.util.ArrayList<>()).build();
      errorLogHandler.writeSummary(0, 0);
      return show;
    }

    List<Episode> episodes = new java.util.ArrayList<>();
    int failureCount = 0;
    for (File mp3File : mp3Files) {
      String filename = mp3File.getName();
      try {
        Episode episode = parseEpisode(filename, show.getLink());
        episodes.add(episode);
      } catch (Exception e) {
        failureCount++;
        String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        logger.warn("Could not parse episode {}, skipping: {}", filename, detail);
        errorLogHandler.writeError(ParseErrorReason.EPISODE_MP3_PARSE_ERROR, filename, detail);
      }
    }

    errorLogHandler.writeSummary(episodes.size(), failureCount);

    show = show.toBuilder().episodes(episodes).build();

    return show;
  }

  public Show parseShow() {
    try {
      String basePath = System.getProperty("user.dir");
      Path showFilePath =
          Path.of(basePath).resolve(rssConfig.getInfoDir()).resolve(rssConfig.getShowFileName());
      File showFile = showFilePath.toFile();

      Show show = objectMapper.readValue(showFile, Show.class);

      if (show.getLanguage() == null || show.getLanguage().isBlank()) {
        throw new IllegalArgumentException("Language is missing or blank in show.json");
      }

      String imageFilename = show.getImage();
      if (imageFilename != null && !imageFilename.isBlank()) {
        String baseFilename = imageFilename;
        int lastDot = imageFilename.lastIndexOf('.');
        if (lastDot > 0) {
          baseFilename = imageFilename.substring(0, lastDot);
        }

        Path artworkFilePath =
            Path.of(System.getProperty("user.dir"))
                .resolve(rssConfig.getArtworkDir())
                .resolve(baseFilename + rssConfig.getArtworkFileExtension());

        if (Files.exists(artworkFilePath) && show.getLink() != null) {
          String encodedFilename =
              URLEncoder.encode(baseFilename, StandardCharsets.UTF_8).replace("+", "%20");
          show =
              show.toBuilder()
                  .image(
                      show.getLink()
                          + "/artwork/"
                          + encodedFilename
                          + rssConfig.getArtworkFileExtension())
                  .build();
        }
      }

      return show;
    } catch (Exception e) {
      String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      String showFileName = rssConfig.getShowFileName();
      String rootCause = getRootCause(e);
      if (rootCause != null && rootCause.contains("No such file")) {
        errorLogHandler.writeError(
            ParseErrorReason.SHOW_CONFIG_FILE_NOT_FOUND, showFileName, e.getMessage());
      } else {
        errorLogHandler.writeError(ParseErrorReason.SHOW_CONFIG_INVALID_JSON, showFileName, detail);
      }
      logger.error("Failed to parse show file", e);
      throw new RuntimeException("Failed to parse show file", e);
    }
  }

  private String getRootCause(Throwable e) {
    Throwable cause = e;
    while (cause.getCause() != null && cause.getCause() != cause) {
      cause = cause.getCause();
    }
    return cause.getMessage();
  }

  public Episode parseEpisode(String episodeFile, String showLink) {
    String episodesDir = rssConfig.getEpisodesDir();
    String artworkDir = rssConfig.getArtworkDir();

    Path filePath =
        Path.of(System.getProperty("user.dir")).resolve(episodesDir).resolve(episodeFile);

    String title = episodeFile;
    String description = "";
    LocalDateTime pubDate = null;
    int duration = 0;

    try {
      long lastModified = Files.getLastModifiedTime(filePath).toMillis();
      pubDate =
          LocalDateTime.ofInstant(
              java.time.Instant.ofEpochMilli(lastModified), ZoneId.systemDefault());
    } catch (IOException e) {
      errorLogHandler.writeError(
          ParseErrorReason.EPISODE_PUB_DATE_MISSING, episodeFile, e.getMessage());
      throw new RuntimeException("Failed to determine publication date: " + episodeFile, e);
    }

    try {
      Mp3File mp3File = new Mp3File(filePath.toFile());
      duration = (int) mp3File.getLengthInSeconds();
      if (mp3File.hasId3v2Tag()) {
        ID3v2 tag = mp3File.getId3v2Tag();
        if (tag != null) {
          String tit2 = tag.getTitle();
          if (tit2 != null && !tit2.isBlank()) {
            title = tit2;
          }
          String tdes = tag.getComment();
          if (tdes != null && !tdes.isBlank()) {
            description = tdes;
          }

          if (rssConfig.getExtractArtwork()) {
            String artworkFilename = episodeFile;
            int lastDot = episodeFile.lastIndexOf('.');
            if (lastDot > 0) {
              artworkFilename = episodeFile.substring(0, lastDot);
            }

            Path artworkFilePath =
                Path.of(System.getProperty("user.dir"))
                    .resolve(artworkDir)
                    .resolve(artworkFilename + rssConfig.getArtworkFileExtension());

            if (!Files.exists(artworkFilePath) && tag.getAlbumImage() != null) {
              String mime = tag.getAlbumImageMimeType();
              String[] acceptedExtensions = MIME_TYPE_MAPPING.getOrDefault(mime, new String[] {});
              if (acceptedExtensions.length == 0) {
                errorLogHandler.writeError(
                    ParseErrorReason.ARTWORK_UNSUPPORTED_MIME,
                    episodeFile,
                    "MIME type " + mime + " has no configured extension");
              } else {
                String configExt = rssConfig.getArtworkFileExtension();
                if (extMatchesMimeType(configExt, mime, acceptedExtensions)) {
                  try {
                    byte[] imageData = tag.getAlbumImage();
                    Files.write(artworkFilePath, imageData);
                  } catch (IOException e) {
                    errorLogHandler.writeError(
                        ParseErrorReason.ARTWORK_WRITE_FAILED, episodeFile, e.getMessage());
                  }
                } else {
                  errorLogHandler.writeError(
                      ParseErrorReason.ARTWORK_MIME_MISMATCH,
                      episodeFile,
                      "MIME type '"
                          + mime
                          + "' doesn't match configured extension '"
                          + configExt
                          + "'");
                }
              }
            }
          }
        }
      }
    } catch (IOException | UnsupportedTagException | InvalidDataException e) {
      String detail = e.getClass().getSimpleName() + " - " + e.getMessage();
      errorLogHandler.writeError(ParseErrorReason.EPISODE_MP3_PARSE_ERROR, episodeFile, detail);
      throw new RuntimeException("Failed to parse MP3 metadata: " + episodeFile, e);
    }

    String filenameNoExt = episodeFile;
    int lastDot = episodeFile.lastIndexOf('.');
    if (lastDot > 0) {
      filenameNoExt = episodeFile.substring(0, lastDot);
    }

    String encodedFilename =
        URLEncoder.encode(episodeFile, StandardCharsets.UTF_8).replace("+", "%20");
    String encodedArtworkFilename =
        URLEncoder.encode(filenameNoExt, StandardCharsets.UTF_8).replace("+", "%20");

    String url = showLink + "/episodes/" + encodedFilename;
    String image =
        showLink + "/artwork/" + encodedArtworkFilename + rssConfig.getArtworkFileExtension();

    long fileSize = 0;
    try {
      fileSize = Files.size(filePath);
    } catch (IOException e) {
      errorLogHandler.writeError(
          ParseErrorReason.EPISODE_FILE_SIZE_UNKNOWN, episodeFile, e.getMessage());
    }

    return Episode.builder()
        .title(title)
        .description(description)
        .url(url)
        .pubDate(pubDate)
        .image(image)
        .enclosureUrl(url)
        .enclosureType("audio/mpeg")
        .enclosureLength(fileSize)
        .duration(duration)
        .build();
  }

  private boolean extMatchesMimeType(String ext, String mime, String[] acceptedExtensions) {
    for (String accepted : acceptedExtensions) {
      if (ext.equalsIgnoreCase(accepted)) {
        return true;
      }
    }
    return false;
  }
}
