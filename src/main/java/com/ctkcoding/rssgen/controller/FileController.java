package com.ctkcoding.rssgen.controller;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.service.FileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.NoSuchFileException;

@RestController
@Slf4j
@AllArgsConstructor
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    FileService fileService;

    @Autowired
    RssConfig rssConfig;

    @GetMapping("/rss")
    public ResponseEntity<byte[]> returnRss() {
        return buildResponse("", rssConfig.getRssFileName(), "text/xml", rssConfig.getRssFileName());
     }

    @GetMapping("/episodes/{episode}")
    public ResponseEntity<byte[]> returnEpisode(@PathVariable String episode) {
        String filename = extractFilename(episode);
        return buildResponse(rssConfig.getEpisodesDir(), episode, "audio/mpeg", filename + ".mp3");
     }

    @GetMapping("/artwork/{artwork}")
    public ResponseEntity<byte[]> returnArtwork(@PathVariable String artwork) {
        String filename = extractFilename(artwork);
        return buildResponse(rssConfig.getArtworkDir(), artwork, "image/jpeg", filename + ".jpeg");
     }

     private ResponseEntity<byte[]> buildResponse(String extraPath, String file, String contentType, String downloadFilename) {
        try {
            byte[] content = fileService.getFile(extraPath, file);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDisposition(ContentDisposition.attachment().filename(downloadFilename).build());
            return ResponseEntity.ok().headers(headers).body(content);
         } catch (IllegalArgumentException e) {
            logger.warn("Path traversal attempt: {}", file);
            return ResponseEntity.badRequest().build();
         } catch (NoSuchFileException e) {
            logger.warn("File not found: {}", file);
            return ResponseEntity.notFound().build();
         } catch (IOException e) {
            logger.error("Error reading file: {}", file, e);
            return ResponseEntity.internalServerError().build();
         }
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
