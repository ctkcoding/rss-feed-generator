package com.ctkcoding.rssgen.controller;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.service.FileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<String> returnRss() {
        // todo - return it as XML isntead of string
        return ResponseEntity.ok(fileService.returnFileIfExists(rssConfig.getRssFileName()));
    }

    @GetMapping("/episode/{episode}")
    public ResponseEntity<String> returnEpisode() {

        // todo - return file if exists
        // todo - find the file at /episodes/{episode}

        // todo - return it as a file instead of string
        return ResponseEntity.ok("");
    }

    @GetMapping("/artwork/{artwork}")
    public ResponseEntity<String> returnArtwork() {

        // todo - return file if exists
        // todo - find the file at /artwork/{artwork}

        // todo - return it as a file instead of string
        return ResponseEntity.ok("");
    }

}