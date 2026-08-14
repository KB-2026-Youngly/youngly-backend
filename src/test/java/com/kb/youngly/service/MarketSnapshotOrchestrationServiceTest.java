package com.kb.youngly.service;

import com.kb.youngly.dto.market.MarketSnapshotIngestResult;
import com.kb.youngly.exception.FssMarketRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MarketSnapshotOrchestrationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-14T01:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private MarketSnapshotIngestService ingestService;
    private MarketSnapshotSummaryService summaryService;
    private MarketSnapshotOrchestrationService service;

    @BeforeEach
    void setUp() {
        ingestService = mock(MarketSnapshotIngestService.class);
        summaryService = mock(MarketSnapshotSummaryService.class);
        service = new MarketSnapshotOrchestrationService(ingestService, summaryService, FIXED_CLOCK);

        when(ingestService.ingest(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new MarketSnapshotIngestResult(1, 1, 1, 0, 0));
    }

    @Test
    void reingestsRecentTwoWeeksWithSingleRangeCallAndSummarizesRecentRange() {
        service.reingestRecentTwoWeeksAndSummarize("STARTUP");

        LocalDate startDate = LocalDate.of(2026, 7, 31);
        LocalDate endDate = LocalDate.of(2026, 8, 13);
        verify(ingestService, times(1)).ingest(startDate, endDate);
        verifyNoMoreInteractions(ingestService);
        verify(summaryService).generateLatestRecentSummaryIfAbsent(startDate, endDate);
    }

    @Test
    void doesNotRetryIngestWhenFssRateLimitReachedAndStillSummarizes() {
        LocalDate startDate = LocalDate.of(2026, 7, 31);
        LocalDate endDate = LocalDate.of(2026, 8, 13);
        when(ingestService.ingest(startDate, endDate))
                .thenThrow(new FssMarketRateLimitException("resultCode=033"));

        assertDoesNotThrow(() -> service.reingestRecentTwoWeeksAndSummarize("STARTUP"));

        verify(ingestService, times(1)).ingest(startDate, endDate);
        verifyNoMoreInteractions(ingestService);
        verify(summaryService).generateLatestRecentSummaryIfAbsent(startDate, endDate);
    }

    @Test
    void doesNotPropagateWhenIngestFailsAndStillSummarizes() {
        LocalDate startDate = LocalDate.of(2026, 7, 31);
        LocalDate endDate = LocalDate.of(2026, 8, 13);
        when(ingestService.ingest(startDate, endDate))
                .thenThrow(new RuntimeException("fss down"));

        assertDoesNotThrow(() -> service.reingestRecentTwoWeeksAndSummarize("STARTUP"));

        verify(ingestService, times(1)).ingest(startDate, endDate);
        verify(summaryService).generateLatestRecentSummaryIfAbsent(startDate, endDate);
    }

    @Test
    void doesNotPropagateWhenSummaryFails() {
        doThrow(new RuntimeException("openai down"))
                .when(summaryService)
                .generateLatestRecentSummaryIfAbsent(any(LocalDate.class), any(LocalDate.class));

        assertDoesNotThrow(() -> service.reingestRecentTwoWeeksAndSummarize("STARTUP"));
        verify(ingestService, times(1)).ingest(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 8, 13));
    }
}
