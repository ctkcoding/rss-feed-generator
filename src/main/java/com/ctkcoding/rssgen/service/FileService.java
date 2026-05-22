package com.ctkcoding.rssgen.service;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
public class FileService {

    @Getter
    private Path basePath = Path.of(System.getProperty("user.dir"));

    private List<String> allowedSubpaths = List.of("artwork", "episodes", "");

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    public String returnFileIfExists(String extraPath, String file) {
        if (!allowedSubpaths.contains(extraPath)) {
            throw new IllegalArgumentException("Path traversal attempt detected");
         }

         Path filePath = normalizePath(basePath, extraPath, file);

         logger.info("resolved full file path" + filePath);

         try {
             if (Files.exists(filePath, LinkOption.NOFOLLOW_LINKS)) {
                 return "file exists: " +  filePath;
              }
          } catch (Exception e) {
             throw new RuntimeException(e);
          }
         return "file not found: " +  filePath;
      }

     private Path normalizePath(Path base, String extraPath, String file) {
        Path normalizedBase = base.toAbsolutePath().normalize();
        Path resolvedPath = base.resolve(extraPath).resolve(file).normalize();

         if (!resolvedPath.startsWith(normalizedBase)) {
             throw new IllegalArgumentException("Path traversal attempt detected");
          }

         return resolvedPath;
     }
}
