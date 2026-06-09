package com.ctkcoding.rssgen.service;

import com.ctkcoding.rssgen.config.RssConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FileService {

  private final RssConfig rssConfig;

  @Getter private Path basePath = Path.of(System.getProperty("user.dir"));

  private List<String> dynamicAllowedSubpaths;

  public FileService(RssConfig rssConfig) {
    this.rssConfig = rssConfig;
    this.dynamicAllowedSubpaths =
        List.of("", rssConfig.getEpisodesDir(), rssConfig.getArtworkDir(), rssConfig.getInfoDir());
  }

  private static final Logger logger = LoggerFactory.getLogger(FileService.class);

  public byte[] getFile(String extraPath, String file) throws IOException {
    if (!dynamicAllowedSubpaths.contains(extraPath)) {
      throw new IllegalArgumentException("Path traversal attempt detected");
    }

    Path filePath = normalizePath(basePath, extraPath, file);
    logger.info("resolved full file path: " + filePath);

    return Files.readAllBytes(filePath);
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
