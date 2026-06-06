package com.ctkcoding.rssgen.service;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.model.Show;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchServiceTest {

    @Mock
    ParseService parseService;

    @Mock
    RssService rssService;

    @Mock
    RssConfig rssConfig;

    @InjectMocks
    WatchService watchService;

    @BeforeEach
    void setUp() {
        lenient().when(rssConfig.getFailureLimit()).thenReturn(5);
        lenient().when(rssConfig.getFileWatch()).thenReturn(false);
        lenient().when(rssConfig.getEpisodesDir()).thenReturn("episodes");
     }

     @Test
    void startWatching_directoryNotFound_throwsIllegalStateException() {
        when(rssConfig.getFileWatch()).thenReturn(true);
        when(rssConfig.getEpisodesDir()).thenReturn("nonexistent_dir_xyz");

        assertThrows(
            IllegalStateException.class,
            () -> watchService.startWatching()
        );
     }

     @Test
    void checkForNewChanges_noChanges_doesNotCallParseOrRss() {
        watchService.newFileChanges.set(false);
        watchService.checkForNewChanges();

        verifyNoInteractions(parseService);
        verifyNoInteractions(rssService);
        assertEquals(0, watchService.failureCounter.get());
     }

     @Test
    void checkForNewChanges_withChanges_succeeds_resetsFlag() {
        Show show = Show.builder()
                 .title("Test Show")
                 .link("https://podcast.local")
                 .language("en-us")
                 .build();

        when(parseService.generateShow()).thenReturn(show);
        when(rssService.writeRss(show)).thenReturn("rss.xml");

        watchService.newFileChanges.set(true);
        watchService.checkForNewChanges();

        verify(parseService, times(1)).generateShow();
        verify(rssService, times(1)).writeRss(show);
        assertFalse(watchService.newFileChanges.get());
        assertEquals(0, watchService.failureCounter.get());
     }

     @Test
    void checkForNewChanges_rssFailure_incrementsCounter_keepsFlagTrue() {
        when(parseService.generateShow()).thenReturn(Show.builder()
                 .title("Test Show").link("https://podcast.local").language("en-us").build());
        when(rssService.writeRss(any())).thenThrow(new RuntimeException("Write failed"));

        watchService.newFileChanges.set(true);
        watchService.checkForNewChanges();

        assertEquals(1, watchService.failureCounter.get());
        assertTrue(watchService.newFileChanges.get());
     }

     @Test
    void checkForNewChanges_failureLimitExceeded_resetsFlag_noMoreRetries() {
        Show show = Show.builder()
                 .title("Test Show").link("https://podcast.local").language("en-us").build();
        when(parseService.generateShow()).thenReturn(show);
        when(rssService.writeRss(any())).thenThrow(new RuntimeException("Write failed"));

        watchService.newFileChanges.set(true);
        watchService.failureCounter.set(5);
        watchService.checkForNewChanges();

        assertEquals(6, watchService.failureCounter.get());
        assertFalse(watchService.newFileChanges.get());
     }

     @Test
    void checkForNewChanges_counterEqualToLimit_doesNotStopRetries() {
        Show show = Show.builder()
                 .title("Test Show").link("https://podcast.local").language("en-us").build();
        when(parseService.generateShow()).thenReturn(show);
        when(rssService.writeRss(any())).thenThrow(new RuntimeException("Write failed"));

        watchService.newFileChanges.set(true);
        watchService.failureCounter.set(4);
        watchService.checkForNewChanges();

        assertTrue(watchService.newFileChanges.get());
        assertEquals(5, watchService.failureCounter.get());
     }

     @Test
    void checkForNewChanges_failureThenSuccess_resetsFlag_keepsCounter() {
        Show show = Show.builder()
                 .title("Test Show").link("https://podcast.local").language("en-us").build();

        when(parseService.generateShow()).thenReturn(show);
        when(rssService.writeRss(any()))
                 .thenThrow(new RuntimeException("Write failed"))
                 .thenReturn("rss.xml");

        watchService.newFileChanges.set(true);
        watchService.checkForNewChanges();

        int counterAfterFailure = watchService.failureCounter.get();
        assertEquals(1, counterAfterFailure);
        assertTrue(watchService.newFileChanges.get());

        watchService.newFileChanges.set(true);
        watchService.checkForNewChanges();

        int counterAfterSuccess = watchService.failureCounter.get();
        assertEquals(1, counterAfterSuccess);
        assertFalse(watchService.newFileChanges.get());
     }
}
