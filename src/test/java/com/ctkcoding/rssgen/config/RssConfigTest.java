package com.ctkcoding.rssgen.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class RssConfigTest {

    @Autowired
    private RssConfig rssConfig;

    @Test
    void defaults() {
        assertEquals(System.getProperty("user.dir"), rssConfig.pathRoot);
        assertEquals("artwork", rssConfig.artworkDir);
        assertEquals("episodes", rssConfig.episodesDir);
        assertEquals("info", rssConfig.infoDir);
        assertEquals("rss.xml", rssConfig.rssFileName);
        assertEquals("show.json", rssConfig.showFileName);
        assertEquals(".mp3", rssConfig.episodeFileExtension);
        assertEquals(".jpeg", rssConfig.artworkFileExtension);
        assertEquals(false, rssConfig.extractArtwork);
        assertEquals(false, rssConfig.fileWatch);
        assertEquals("us-en", rssConfig.language);
    }
}
