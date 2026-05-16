package com.ctkcoding.rssgen.controller;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired
    private FileService fileService;

    @MockitoBean
    private RssConfig rssConfig;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(rssConfig.getEpisodesDir()).thenReturn("episodes");
        when(rssConfig.getArtworkDir()).thenReturn("artwork");
    }

    @Test
    void rss_returnsOkWhenFileExists() throws Exception {
        when(rssConfig.getRssFileName()).thenReturn("rss.xml");

        mockMvc.perform(get("/rss"))
                .andExpect(status().isOk())
                .andExpect(content().string("file exists: " + fileService.getBasePath() +  "/rss.xml"));
    }

    @Test
    void episode_returnsOkWhenFileExists() throws Exception {
        mockMvc.perform(get("/episodes/01 Episode 1.mp3"))
                .andExpect(status().isOk())
                .andExpect(content().string("file exists: " + fileService.getBasePath() +  "/episodes/01 Episode 1.mp3"));
    }

    @Test
    void artwork_returnsOkWhenFileExists() throws Exception {
        mockMvc.perform(get("/artwork/01 Episode 1.jpeg"))
                .andExpect(status().isOk())
                .andExpect(content().string("file exists: " + fileService.getBasePath() +  "/artwork/01 Episode 1.jpeg"));
    }

    @Test
    void rss_returnsFileNotFoundWhenFileDoesNotExist() throws Exception {
        when(rssConfig.getRssFileName()).thenReturn("missing.xml");

        mockMvc.perform(get("/rss"))
                .andExpect(status().isOk())
                .andExpect(content().string("file not found: " + fileService.getBasePath() +  "/missing.xml"));
    }

    @Test
    void episode_returnsFileNotFoundWhenFileDoesNotExist() throws Exception {
        mockMvc.perform(get("/episodes/missing.mp3"))
                .andExpect(status().isOk())
                .andExpect(content().string("file not found: " + fileService.getBasePath() +  "/episodes/missing.mp3"));
    }

    @Test
    void artwork_returnsFileNotFoundWhenFileDoesNotExist() throws Exception {
                mockMvc.perform(get("/artwork/missing.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().string("file not found: " + fileService.getBasePath() +  "/artwork/missing.jpg"));
    }
}
