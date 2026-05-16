package com.ctkcoding.rssgen.service;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

@Slf4j
@Service
public class FileService {

    @Getter
    private Path basePath = Path.of(System.getProperty("user.dir"));

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    public String returnFileIfExists(String extraPath, String file) {
        Path filePath = basePath.resolve(extraPath)
                .resolve(file);
        logger.info("resolved full file path" + filePath);

        try {
            if (Files.exists(filePath, LinkOption.NOFOLLOW_LINKS)) {
                // todo - return the actual file
                // todo - reject if the extra path escapes directory
                return "file exists: " +  filePath;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "file not found: " +  filePath;
    }
}
