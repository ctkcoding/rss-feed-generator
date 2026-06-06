package com.ctkcoding.rssgen.controller;

import com.ctkcoding.rssgen.model.Show;
import com.ctkcoding.rssgen.service.ParseService;
import com.ctkcoding.rssgen.service.RssService;
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
public class ParseController {

  @Autowired ParseService parseService;

  @Autowired RssService rssService;

  private static final Logger logger = LoggerFactory.getLogger(ParseController.class);

  @GetMapping("/parse")
  public ResponseEntity<String> returnRss() {

    Show show = parseService.generateShow();
    try {
      String path = rssService.writeRss(show);
      logger.info("RSS feed written to: {}", path);
    } catch (Exception e) {
      logger.error("Failed to generate RSS feed: {}", e.getMessage(), e);
    }
    return ResponseEntity.ok("Starting a new parse");
  }
}
