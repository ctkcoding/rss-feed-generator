package com.ctkcoding.rssgen.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.service.FileService;
import java.io.ByteArrayInputStream;
import java.nio.file.NoSuchFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

  @MockitoBean private FileService fileService;

  @MockitoBean private RssConfig rssConfig;

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    when(rssConfig.getEpisodesDir()).thenReturn("episodes");
    when(rssConfig.getArtworkDir()).thenReturn("artwork");
    when(rssConfig.getInfoDir()).thenReturn("info");
  }

  @Test
  void rss_returnsOkWhenFileExists() throws Exception {
    byte[] expectedContent = "some xml content".getBytes();
    when(fileService.getFile("info", "rss.xml"))
        .thenReturn(new ByteArrayInputStream(expectedContent));
    when(rssConfig.getRssFileName()).thenReturn("rss.xml");

    mockMvc
        .perform(get("/rss"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/xml"))
        .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=")))
        .andExpect(content().bytes(expectedContent));
  }

  @Test
  void episode_returnsOkWhenFileExists() throws Exception {
    byte[] expectedContent = "mp3 audio data".getBytes();
    when(fileService.getFile("episodes", "01 Episode 1.mp3"))
        .thenReturn(new ByteArrayInputStream(expectedContent));

    mockMvc
        .perform(get("/episodes/01 Episode 1.mp3"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("audio/mpeg"))
        .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=")))
        .andExpect(content().bytes(expectedContent));
  }

  @Test
  void artwork_returnsOkWhenFileExists() throws Exception {
    byte[] expectedContent = "jpeg image data".getBytes();
    when(fileService.getFile("artwork", "01 Episode 1.jpeg"))
        .thenReturn(new ByteArrayInputStream(expectedContent));

    mockMvc
        .perform(get("/artwork/01 Episode 1.jpeg"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/jpeg"))
        .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=")))
        .andExpect(content().bytes(expectedContent));
  }

  @Test
  void rss_returnsNotFoundWhenFileDoesNotExist() throws Exception {
    when(rssConfig.getRssFileName()).thenReturn("missing.xml");
    when(fileService.getFile("info", "missing.xml"))
        .thenThrow(new NoSuchFileException("missing.xml"));

    mockMvc.perform(get("/rss")).andExpect(status().isNotFound());
  }

  @Test
  void episode_returnsNotFoundWhenFileDoesNotExist() throws Exception {
    when(fileService.getFile("episodes", "missing.mp3"))
        .thenThrow(new NoSuchFileException("missing.mp3"));

    mockMvc.perform(get("/episodes/missing.mp3")).andExpect(status().isNotFound());
  }

  @Test
  void artwork_returnsNotFoundWhenFileDoesNotExist() throws Exception {
    when(fileService.getFile("artwork", "missing.jpg"))
        .thenThrow(new NoSuchFileException("missing.jpg"));

    mockMvc.perform(get("/artwork/missing.jpg")).andExpect(status().isNotFound());
  }
}
