package com.ctkcoding.rssgen.controller;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.service.FileService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@AllArgsConstructor
public class FileController {

  private static final Logger logger = LoggerFactory.getLogger(FileController.class);

  @Autowired FileService fileService;

  @Autowired RssConfig rssConfig;

  @GetMapping("/rss")
  public ResponseEntity<InputStreamResource> returnRss() throws IOException {
    return buildResponse(
        rssConfig.getInfoDir(), rssConfig.getRssFileName(), "text/xml", rssConfig.getRssFileName());
  }

  @GetMapping("/episodes/{episode}")
  public ResponseEntity<InputStreamResource> returnEpisode(@PathVariable String episode)
      throws IOException {
    String filename = extractFilename(episode);
    return buildResponse(
        rssConfig.getEpisodesDir(),
        episode,
        "audio/mpeg",
        filename + rssConfig.getEpisodeFileExtension());
  }

  @GetMapping("/artwork/{artwork}")
  public ResponseEntity<InputStreamResource> returnArtwork(@PathVariable String artwork)
      throws IOException {
    String filename = extractFilename(artwork);
    return buildResponse(
        rssConfig.getArtworkDir(),
        artwork,
        "image/jpeg",
        filename + rssConfig.getArtworkFileExtension());
  }

  private ResponseEntity<InputStreamResource> buildResponse(
      String extraPath, String file, String contentType, String downloadFilename)
      throws IOException {
    Path filePath = normalizePath(extraPath, file);
    InputStream inputStream = fileService.getFile(extraPath, file);
    InputStreamResource resource = new InputStreamResource(inputStream);
    ResponseEntity.BodyBuilder responseBuilder =
        ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType));

    try {
      long size = Files.size(filePath);
      responseBuilder.contentLength(size);
    } catch (java.nio.file.NoSuchFileException e) {
      // In test/mock contexts the file path may not exist on disk
    }

    return responseBuilder
        .header(
            "Content-Disposition",
            ContentDisposition.attachment().filename(downloadFilename).build().toString())
        .body(resource);
  }

  private Path normalizePath(String extraPath, String file) {
    return Path.of(System.getProperty("user.dir")).resolve(extraPath).resolve(file).normalize();
  }

  private String extractFilename(String slug) {
    if (slug.contains("/") || slug.contains("..")) {
      throw new IllegalArgumentException("Invalid filename");
    }
    int lastDot = slug.lastIndexOf('.');
    if (lastDot > 0) {
      return slug.substring(0, lastDot);
    }
    return slug;
  }
}
