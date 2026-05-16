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

    public String returnFileIfExists(String file) {
        Path filePath = basePath.resolve(file);
        logger.info("resolved full file path" + filePath);

        // todo - return the actual file
        //  return null if file isn't ready/doesn't exist
        //  or RE PARSE UNLESS SET NOT TO

        // todo - create PENDING or don't return at all if not exists
//        try {
//            return Files.exists(filePath, LinkOption.NOFOLLOW_LINKS);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
        return filePath.toString();
    }
}
