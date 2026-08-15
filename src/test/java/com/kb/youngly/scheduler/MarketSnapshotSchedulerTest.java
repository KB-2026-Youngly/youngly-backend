package com.kb.youngly.scheduler;

import com.kb.youngly.service.MarketSnapshotOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class MarketSnapshotSchedulerTest {

    private MarketSnapshotOrchestrationService orchestrationService;
    private MarketSnapshotScheduler scheduler;

    @BeforeEach
    void setUp() {
        orchestrationService = mock(MarketSnapshotOrchestrationService.class);
        scheduler = new MarketSnapshotScheduler(orchestrationService);
    }

    @Test
    void delegatesToSharedOrchestrationService() {
        scheduler.ingestRecentMarketSnapshots();

        verify(orchestrationService).reingestRecentTwoWeeksAndSummarize("SCHEDULER");
    }

    @Test
    void doesNotPropagateWhenOrchestrationFails() {
        when(orchestrationService.reingestRecentTwoWeeksAndSummarize("SCHEDULER"))
                .thenThrow(new RuntimeException("fss unavailable"));

        assertDoesNotThrow(() -> scheduler.ingestRecentMarketSnapshots());
    }
}
