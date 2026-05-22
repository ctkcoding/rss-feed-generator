package com.ctkcoding.rssgen.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.nio.file.NoSuchFileException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

     @ExceptionHandler(NoSuchFileException.class)
    public ResponseEntity<Void> handleNoSuchFile(NoSuchFileException e) {
        return ResponseEntity.notFound().build();
    }

     @ExceptionHandler(IOException.class)
    public ResponseEntity<Void> handleIOException(IOException e) {
        logger.error("Error reading file", e);
        return ResponseEntity.internalServerError().build();
    }

     @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.badRequest().build();
    }
}
