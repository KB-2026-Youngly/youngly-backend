package com.kb.youngly.scheduler;

import com.kb.youngly.dto.market.MarketSnapshotIngestResult;
import com.kb.youngly.service.MarketSnapshotIngestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class MarketSnapshotSchedulerTest {

    private MarketSnapshotIngestService marketSnapshotIngestService;
    private MarketSnapshotScheduler scheduler;

    @BeforeEach
    void setUp() {
        marketSnapshotIngestService = mock(MarketSnapshotIngestService.class);
        scheduler = new MarketSnapshotScheduler(marketSnapshotIngestService);
    }

    @Test
    void callsIngestServiceOnSuccess() {
        when(marketSnapshotIngestService.ingestRecentTwoWeeks())
                .thenReturn(new MarketSnapshotIngestResult(3, 2, 2, 0, 0));

        scheduler.ingestRecentMarketSnapshots();

        verify(marketSnapshotIngestService).ingestRecentTwoWeeks();
    }

    @Test
    void doesNotPropagateExceptionWhenIngestFails() {
        when(marketSnapshotIngestService.ingestRecentTwoWeeks())
                .thenThrow(new RuntimeException("fss unavailable"));

        assertDoesNotThrow(() -> scheduler.ingestRecentMarketSnapshots());
        verify(marketSnapshotIngestService).ingestRecentTwoWeeks();
    }
}
