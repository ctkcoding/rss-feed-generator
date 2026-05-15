package com.ctkcoding.rssgen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component // Makes it a Spring Bean so binding can occur
@ConfigurationProperties(prefix = "rss")
public class RssConfig {
    String pathRoot = System.getProperty("user.dir");
    String artworkDir = "artwork";
    String episodesDir = "episodes";
    String infoDir = "info";

    // todo - allow override by application.properties
    String rssFileName = "rss.xml";
    String showFileName = "show.json";
    String episodeFileExtension = ".mp3";
    String artworkFileExtension = ".jpeg";

    Boolean extractArtwork = false;
    Boolean fileWatch = false;

    String language = "us-en";
}
