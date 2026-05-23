package com.ctkcoding.rssgen.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties") // or file:/absolute/path/to/test.properties
class RssConfigTest {

    @Autowired
    private RssConfig rssConfig;

    @Test
    void propertiesTest() {
        assertEquals("artwork", rssConfig.getArtworkDir());
        assertEquals("episodes", rssConfig.getEpisodesDir());
        assertEquals("info", rssConfig.getInfoDir());
        assertEquals("rss.xml", rssConfig.getRssFileName());
        assertEquals("show.json", rssConfig.getShowFileName());
        assertEquals(".mp3", rssConfig.getEpisodeFileExtension());
        assertEquals(".jpeg", rssConfig.getArtworkFileExtension());
        assertEquals(false, rssConfig.getExtractArtwork());
         assertEquals(false, rssConfig.getFileWatch());
         assertEquals("en-us", rssConfig.getLanguage());
         assertEquals("parse-errors.log", rssConfig.getErrorLogFile());
      }

}
