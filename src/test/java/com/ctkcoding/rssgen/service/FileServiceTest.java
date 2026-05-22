package com.ctkcoding.rssgen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileServiceTest {

     private FileService fileService;
     private String testResourcesPath;

     @BeforeEach
    void setUp() throws URISyntaxException, IllegalArgumentException {
        fileService = new FileService();
        String rssXmlPath = getClass().getResource("/rss.xml").toURI().getPath();
        testResourcesPath = Path.of(rssXmlPath).getParent().toString();
        setBasePath(Path.of(testResourcesPath).toAbsolutePath());
          }

         @Test
     void returnsRssFile() {
          fileService = new FileService();
          setBasePath(Path.of(testResourcesPath).toAbsolutePath());

          String result = fileService.returnFileIfExists("", "rss.xml");
          assertTrue(result.contains("file exists:"));
          assertTrue(result.endsWith("rss.xml"));
          }

         @Test
     void returnsEpisodeFile() {
          fileService = new FileService();
          setBasePath(Path.of(testResourcesPath).toAbsolutePath());

          String result = fileService.returnFileIfExists("episodes", "Episode 01.mp3");
          assertTrue(result.contains("file exists:"));
          assertTrue(result.endsWith("Episode 01.mp3"));
          }

         @Test
     void returnsArtworkFile() {
          fileService = new FileService();
          setBasePath(Path.of(testResourcesPath).toAbsolutePath());

          String result = fileService.returnFileIfExists("artwork", "Episode 01.jpeg");
          assertTrue(result.contains("file exists:"));
          assertTrue(result.endsWith("Episode 01.jpeg"));
          }

         @Test
     void returnsNotFoundForMissingRss() {
          fileService = new FileService();
          setBasePath(Path.of(testResourcesPath).toAbsolutePath());

          String result = fileService.returnFileIfExists("", "nonexistent.xml");
          assertEquals("file not found: " + testResourcesPath + "/nonexistent.xml", result);
          }

         @Test
     void returnsNotFoundForMissingEpisode() {
          fileService = new FileService();
          setBasePath(Path.of(testResourcesPath).toAbsolutePath());

          String result = fileService.returnFileIfExists("episodes", "missing.mp3");
          assertTrue(result.contains("file not found:"));
          assertTrue(result.endsWith("episodes/missing.mp3"));
          }

         @Test
     void returnsNotFoundForMissingArtwork() {
          fileService = new FileService();
          setBasePath(Path.of(testResourcesPath).toAbsolutePath());

          String result = fileService.returnFileIfExists("artwork", "missing.jpg");
          assertTrue(result.contains("file not found:"));
          assertTrue(result.endsWith("artwork/missing.jpg"));
          }

           @Test
     void rejectsPathTraversalInFileName() throws URISyntaxException {
          fileService = new FileService();
          setBasePath(Path.of(testResourcesPath).toAbsolutePath());

          String path = "episodes" + java.io.File.separator + ".." + java.io.File.separator + ".." + java.io.File.separator + ".." + java.io.File.separator + ".." + java.io.File.separator + "rss.xml";
          assertThrows(IllegalArgumentException.class,
                    () -> fileService.returnFileIfExists("episodes", path));
        }

         @Test
     void rejectsPathTraversalInExtraPath() {
          fileService = new FileService();
          setBasePath(Path.of(testResourcesPath).toAbsolutePath());

          assertThrows(IllegalArgumentException.class,
                  () -> fileService.returnFileIfExists("../../artwork", "../rss.xml"));
          }

         @Test
     void rejectsPathTraversalInBothFields() {
          fileService = new FileService();
          setBasePath(Path.of(testResourcesPath).toAbsolutePath());

          assertThrows(IllegalArgumentException.class,
                  () -> fileService.returnFileIfExists("../artwork", "../rss.xml"));
          }

         @Test
     void allowsDotsInFilename() {
          fileService = new FileService();
          setBasePath(Path.of(testResourcesPath).toAbsolutePath());

          String result = fileService.returnFileIfExists("episodes", "Episode 01.mp3");
          assertTrue(result.contains("file exists:"));
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
