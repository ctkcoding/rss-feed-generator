package com.ctkcoding.rssgen.service;

import static org.junit.jupiter.api.Assertions.*;

import com.ctkcoding.rssgen.config.RssConfig;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileServiceTest {

  private FileService fileService;
  private Path testResourcesDir;
  private RssConfig rssConfig;

  @BeforeEach
  void setUp() throws URISyntaxException {
    rssConfig =
        new RssConfig(
            "info",
            "episodes",
            "artwork",
            "rss.xml",
            "show.xml",
            ".mp3",
            ".jpeg",
            "en",
            true,
            true,
            true,
            5,
            "parse-errors.log");
    fileService = new FileService(rssConfig);
    String rssXmlPath = getClass().getResource("/rss.xml").toURI().getPath();
    testResourcesDir = Path.of(rssXmlPath).getParent();
    setBasePath(testResourcesDir);
  }

  @Test
  void returnsRssFile() throws IOException {
    byte[] result = fileService.getFile("", "rss.xml");
    assertTrue(result.length > 0);
  }

  @Test
  void returnsEpisodeFile() throws IOException {
    byte[] result = fileService.getFile("episodes", "Episode 01.mp3");
    assertTrue(result.length > 0);
  }

  @Test
  void returnsArtworkFile() throws IOException {
    byte[] result = fileService.getFile("artwork", "Episode 01.jpeg");
    assertTrue(result.length > 0);
  }

  @Test
  void returnsNotFoundForMissingRss() throws IOException {
    assertThrows(NoSuchFileException.class, () -> fileService.getFile("", "nonexistent.xml"));
  }

  @Test
  void returnsNotFoundForMissingEpisode() throws IOException {
    assertThrows(NoSuchFileException.class, () -> fileService.getFile("episodes", "missing.mp3"));
  }

  @Test
  void returnsNotFoundForMissingArtwork() throws IOException {
    assertThrows(NoSuchFileException.class, () -> fileService.getFile("artwork", "missing.jpg"));
  }

  @Test
  void rejectsPathTraversalInFileName() throws URISyntaxException {
    String path =
        "episodes"
            + java.io.File.separator
            + ".."
            + java.io.File.separator
            + ".."
            + java.io.File.separator
            + ".."
            + java.io.File.separator
            + ".."
            + java.io.File.separator
            + "rss.xml";
    assertThrows(IllegalArgumentException.class, () -> fileService.getFile("episodes", path));
  }

  @Test
  void rejectsPathTraversalInExtraPath() throws URISyntaxException {
    assertThrows(
        IllegalArgumentException.class, () -> fileService.getFile("../../artwork", "../rss.xml"));
  }

  @Test
  void rejectsPathTraversalInBothFields() throws URISyntaxException {
    assertThrows(
        IllegalArgumentException.class, () -> fileService.getFile("../artwork", "../rss.xml"));
  }

  @Test
  void allowsDotsInFilename() throws IOException {
    byte[] result = fileService.getFile("episodes", "Episode 01.mp3");
    assertTrue(result.length > 0);
  }

  private void setBasePath(Path path) {
    try {
      java.lang.reflect.Field f = FileService.class.getDeclaredField("basePath");
      f.setAccessible(true);
      f.set(fileService, path);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set basePath", e);
    }
  }
}
