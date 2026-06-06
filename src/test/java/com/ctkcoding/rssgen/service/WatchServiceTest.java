package com.ctkcoding.rssgen.service;

import com.ctkcoding.rssgen.config.RssConfig;
import com.ctkcoding.rssgen.model.Show;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WatchServiceTest {

    private ParseService mockParseService;
    private RssService mockRssService;
    private RssConfig mockRssConfig;

    @BeforeEach
    void setUp() {
        mockParseService = mock(ParseService.class);
        mockRssService = mock(RssService.class);
        mockRssConfig = mock(RssConfig.class);
        when(mockRssConfig.getFailureLimit()).thenReturn(5);
     }

    private WatchService createWatchService() {
        return new WatchService();
    }

    private void injectServiceFields(WatchService service, ParseService parseService, RssService rssService, RssConfig rssConfig) {
        try {
            var parseField = WatchService.class.getDeclaredField("parseService");
            parseField.setAccessible(true);
            parseField.set(service, parseService);

            var rssField = WatchService.class.getDeclaredField("rssService");
            rssField.setAccessible(true);
            rssField.set(service, rssService);

            var configField = WatchService.class.getDeclaredField("rssConfig");
            configField.setAccessible(true);
            configField.set(service, rssConfig);
        } catch (Exception e) {
            fail("Failed to inject service fields: " + e.getMessage());
        }
    }

    private void setNewFileChanges(WatchService service, boolean value) {
        try {
            var field = WatchService.class.getDeclaredField("newFileChanges");
            field.setAccessible(true);
            field.set(service, new java.util.concurrent.atomic.AtomicBoolean(value));
        } catch (Exception e) {
            fail("Failed to set newFileChanges: " + e.getMessage());
        }
    }

    private void setFailureCounter(WatchService service, int value) {
        try {
            var field = WatchService.class.getDeclaredField("failureCounter");
            field.setAccessible(true);
            field.set(service, new java.util.concurrent.atomic.AtomicInteger(value));
        } catch (Exception e) {
            fail("Failed to set failureCounter: " + e.getMessage());
        }
    }

    @Test
    void checkForNewChanges_noChanges_doesNotCallParseOrRss() throws Exception {
        WatchService service = createWatchService();
        injectServiceFields(service, mockParseService, mockRssService, mockRssConfig);
        setNewFileChanges(service, false);

        service.checkForNewChanges();

        verifyNoInteractions(mockParseService);
        verifyNoInteractions(mockRssService);
        var counter = getFailureCounter(service);
        assertEquals(0, counter);
    }

    @Test
    void checkForNewChanges_withChanges_succeeds_resetsFlag() throws Exception {
        Show show = Show.builder()
                .title("Test Show")
                .link("https://podcast.local")
                .language("en-us")
                .build();

        WatchService service = createWatchService();
        when(mockParseService.generateShow()).thenReturn(show);
        when(mockRssService.writeRss(show)).thenReturn("rss.xml");

        injectServiceFields(service, mockParseService, mockRssService, mockRssConfig);
        setNewFileChanges(service, true);
        when(mockRssConfig.getFailureLimit()).thenReturn(5);

        service.checkForNewChanges();

        verify(mockParseService, times(1)).generateShow();
        verify(mockRssService, times(1)).writeRss(show);
        var changes = getNewFileChanges(service);
        assertFalse(changes.get());
        var counter = getFailureCounter(service);
        assertEquals(0, counter);
    }

    @Test
    void checkForNewChanges_rssFailure_incrementsCounter_keepsFlagTrue() throws Exception {
        WatchService service = createWatchService();
        when(mockParseService.generateShow()).thenReturn(Show.builder()
                .title("Test Show").link("https://podcast.local").language("en-us").build());
        when(mockRssService.writeRss(any())).thenThrow(new RuntimeException("Write failed"));

        injectServiceFields(service, mockParseService, mockRssService, mockRssConfig);
        setNewFileChanges(service, true);
        when(mockRssConfig.getFailureLimit()).thenReturn(5);

        service.checkForNewChanges();

        var counter = getFailureCounter(service);
        assertEquals(1, counter);
        var changes = getNewFileChanges(service);
        assertTrue(changes.get());
    }

    @Test
    void checkForNewChanges_failureLimitExceeded_resetsFlag_noMoreRetries() throws Exception {
        WatchService service = createWatchService();
        Show show = Show.builder()
                .title("Test Show").link("https://podcast.local").language("en-us").build();
        when(mockParseService.generateShow()).thenReturn(show);
        when(mockRssService.writeRss(any())).thenThrow(new RuntimeException("Write failed"));

        injectServiceFields(service, mockParseService, mockRssService, mockRssConfig);
        setNewFileChanges(service, true);
        setFailureCounter(service, 5);
        when(mockRssConfig.getFailureLimit()).thenReturn(5);

        service.checkForNewChanges();

        var counter = getFailureCounter(service);
        assertEquals(6, counter);
        var changes = getNewFileChanges(service);
        assertFalse(changes.get());
    }

    @Test
    void checkForNewChanges_counterEqualToLimit_doesNotStopRetries() throws Exception {
        WatchService service = createWatchService();
        Show show = Show.builder()
                 .title("Test Show").link("https://podcast.local").language("en-us").build();
        when(mockParseService.generateShow()).thenReturn(show);
        when(mockRssService.writeRss(any())).thenThrow(new RuntimeException("Write failed"));

        injectServiceFields(service, mockParseService, mockRssService, mockRssConfig);
        setNewFileChanges(service, true);
        setFailureCounter(service, 4); // exactly at limit - 1, so after +1 = 5 which is NOT > 5
        when(mockRssConfig.getFailureLimit()).thenReturn(5);

        service.checkForNewChanges();

        assertTrue(getNewFileChanges(service).get());
        assertEquals(5, getFailureCounter(service));
    }

    @Test
    void checkForNewChanges_failureThenSuccess_resetsFlag_keepsCounter() throws Exception {
        WatchService service = createWatchService();
        Show show = Show.builder()
                .title("Test Show").link("https://podcast.local").language("en-us").build();

        when(mockParseService.generateShow()).thenReturn(show);
        when(mockRssService.writeRss(any()))
                .thenThrow(new RuntimeException("Write failed"))
                .thenReturn("rss.xml");

        injectServiceFields(service, mockParseService, mockRssService, mockRssConfig);
        setNewFileChanges(service, true);
        when(mockRssConfig.getFailureLimit()).thenReturn(5);

        // First call fails
        service.checkForNewChanges();
        int counterAfterFailure = getFailureCounter(service);
        assertEquals(1, counterAfterFailure);
        assertTrue(getNewFileChanges(service).get());

        // Reset the flag (simulate manual intervention or new detection)
        setNewFileChanges(service, true);

        // Second call succeeds
        service.checkForNewChanges();
        int counterAfterSuccess = getFailureCounter(service);
        assertEquals(1, counterAfterSuccess);
        assertFalse(getNewFileChanges(service).get());
    }

    @Test
    void checkForNewChanges_concurrentCalls_noException_flagResets() throws Exception {
        WatchService service = createWatchService();
        Show show = Show.builder()
                .title("Test Show").link("https://podcast.local").language("en-us").build();
        when(mockParseService.generateShow()).thenReturn(show);
        when(mockRssService.writeRss(any())).thenReturn("rss.xml");
        when(mockRssConfig.getFailureLimit()).thenReturn(5);

        injectServiceFields(service, mockParseService, mockRssService, mockRssConfig);
        setNewFileChanges(service, true);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        Runnable task = () -> {
            try {
                startLatch.await();
                service.checkForNewChanges();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(task);
        executor.submit(task);

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertFalse(getNewFileChanges(service).get());
    }

    @Test
    void checkForNewChanges_concurrentFailures_counterIncrementsCorrectly() throws Exception {
        WatchService service = createWatchService();
        when(mockParseService.generateShow()).thenReturn(Show.builder()
                .title("Test Show").link("https://podcast.local").language("en-us").build());
        when(mockRssService.writeRss(any())).thenThrow(new RuntimeException("Write failed"));
        when(mockRssConfig.getFailureLimit()).thenReturn(100);

        injectServiceFields(service, mockParseService, mockRssService, mockRssConfig);
        setNewFileChanges(service, true);

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    service.checkForNewChanges();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        int counter = getFailureCounter(service);
        assertTrue(counter >= numThreads);
    }

    @Test
    void checkForNewChanges_emptyEpisodes_succeeds_resetsFlag() throws Exception {
        WatchService service = createWatchService();
        Show show = Show.builder()
                .title("Test Show")
                .link("https://podcast.local")
                .language("en-us")
                .episodes(new ArrayList<>())
                .build();
        when(mockParseService.generateShow()).thenReturn(show);
        when(mockRssService.writeRss(any())).thenReturn("rss.xml");

        injectServiceFields(service, mockParseService, mockRssService, mockRssConfig);
        setNewFileChanges(service, true);
        when(mockRssConfig.getFailureLimit()).thenReturn(5);

        service.checkForNewChanges();

        verify(mockParseService, times(1)).generateShow();
        verify(mockRssService, times(1)).writeRss(show);
        assertFalse(getNewFileChanges(service).get());
    }


    private AtomicBoolean getNewFileChanges(WatchService service) throws Exception {
        var field = WatchService.class.getDeclaredField("newFileChanges");
        field.setAccessible(true);
        return (AtomicBoolean) field.get(service);
    }

    private int getFailureCounter(WatchService service) throws Exception {
        var field = WatchService.class.getDeclaredField("failureCounter");
        field.setAccessible(true);
        return ((AtomicInteger) field.get(service)).get();
    }
}
