package com.ctkcoding.rssgen.service;

import com.ctkcoding.rssgen.config.RssConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ParseService {
  private static final Logger logger = LoggerFactory.getLogger(ParseService.class);

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private RssConfig rssConfig;

  public Show generateShow() {
    Show show = parseShow();

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
      return show;
    }

    List<Episode> episodes = new java.util.ArrayList<>();
    List<String> errors = new java.util.ArrayList<>();
    for (File mp3File : mp3Files) {
      String filename = mp3File.getName();
      try {
        Episode episode = parseEpisode(filename, show.getLink());
        episodes.add(episode);
      } catch (Exception e) {
        logger.warn("Could not parse episode {}, skipping: {}", filename, e.getMessage());
        errors.add(filename + " - " + e.getMessage());
      }
    }

    if (!errors.isEmpty()) {
      String basePath = System.getProperty("user.dir");
      String errorLogPath = basePath + "/" + rssConfig.getErrorLogFile();
      try {
        Path errorLogFile = Path.of(errorLogPath);
        Path errorLogDir = errorLogFile.getParent();
        if (errorLogDir != null) {
          Files.createDirectories(errorLogDir);
        }
        Files.writeString(
            errorLogFile,
            String.join(System.lineSeparator(), errors) + System.lineSeparator(),
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.APPEND);
        logger.info("Wrote {} parsing errors to {}", errors.size(), errorLogPath);
      } catch (IOException e) {
        logger.error("Failed to write error log: {}", errorLogPath, e);
      }
    }

    show = show.toBuilder().episodes(episodes).build();

    return show;
  }

  public Show parseShow() {
    try {
      String basePath = System.getProperty("user.dir");
      String showFilePath =
          basePath + "/" + rssConfig.getInfoDir() + "/" + rssConfig.getShowFileName();
      File showFile = new File(showFilePath);

      Show show = objectMapper.readValue(showFile, Show.class);

      if (show.getLanguage() == null || show.getLanguage().isBlank()) {
        throw new IllegalArgumentException("Language is missing or blank in show.json");
      }

      return show;
    } catch (Exception e) {
      logger.error("Failed to parse show file", e);
      throw new RuntimeException("Failed to parse show file", e);
    }
  }

  public Episode parseEpisode(String episodeFile, String showLink) {
    String episodesDir = rssConfig.getEpisodesDir();

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
        }
      }
    } catch (IOException | UnsupportedTagException | InvalidDataException e) {
      logger.warn("Could not read MP3 metadata for: {}", episodeFile, e);
      throw new RuntimeException("Failed to parse MP3 metadata for: " + episodeFile, e);
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
      logger.warn("Could not determine file size for: {}", episodeFile, e);
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

  public Boolean artworkExists(String episodeFile) {
    if (!rssConfig.getExtractArtwork()) {
      return false;
    }
    String artworkFilename = episodeFile;
    int lastDot = episodeFile.lastIndexOf('.');
    if (lastDot > 0) {
      artworkFilename = episodeFile.substring(0, lastDot);
    }
    Path artworkPath =
        Path.of(System.getProperty("user.dir"))
            .resolve(rssConfig.getArtworkDir())
            .resolve(artworkFilename + rssConfig.getArtworkFileExtension());
    return Files.exists(artworkPath);
  }

  public void generateArtwork(String artworkPath) {
    if (!rssConfig.getExtractArtwork()) {
      return;
    }
    File file = new File(artworkPath);
    try {
      Mp3File mp3File = new Mp3File(file);
      if (mp3File.hasId3v2Tag()) {
        ID3v2 tag = mp3File.getId3v2Tag();
        if (tag != null && tag.getAlbumImage() != null) {
          byte[] imageData = tag.getAlbumImage();
          String artworkFilePath = artworkPath;
          int lastDot = artworkPath.lastIndexOf('.');
          if (lastDot > 0) {
            artworkFilePath = artworkPath.substring(0, lastDot);
          }
          String artworkFull = artworkFilePath + rssConfig.getArtworkFileExtension();
          Files.write(Path.of(artworkFull), imageData);
        }
      }
    } catch (IOException | UnsupportedTagException | InvalidDataException e) {
      logger.error("Failed to generate artwork from: {}", artworkPath, e);
    }
  }
}
