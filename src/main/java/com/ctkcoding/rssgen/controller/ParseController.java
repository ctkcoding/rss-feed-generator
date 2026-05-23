package com.ctkcoding.rssgen.controller;

import com.ctkcoding.rssgen.model.Show;
import com.ctkcoding.rssgen.service.ParseService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@AllArgsConstructor
public class ParseController {

    @Autowired
    ParseService parseService;

    @GetMapping("/parse")
    public ResponseEntity<String> returnRss() {

        Show show = parseService.generateShow();
        return ResponseEntity.ok("Starting a new parse");
    }
}
