package com.ctkcoding.rssgen.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rss")
@AllArgsConstructor
@Getter
public class RssConfig {
  // todo - use pathRoot user.dir in services where it's needed
  // private String pathRoot = System.getProperty("user.dir");

  private String artworkDir;
  private String episodesDir;
  private String infoDir;

  private String rssFileName;
  private String showFileName;
  private String episodeFileExtension;
  private String artworkFileExtension;
  private String language;

  private Boolean extractArtwork;
  private Boolean fileWatch;
  private Boolean runOnStartup;
  private int failureLimit;

  private String errorLogFilePrefix = "parse-errors-";
}
