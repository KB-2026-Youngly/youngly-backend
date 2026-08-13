package com.kb.youngly.scheduler;

import com.kb.youngly.dto.market.MarketSnapshotIngestResult;
import com.kb.youngly.service.MarketSnapshotIngestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MarketSnapshotSchedulerTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-13T10:00:00Z"),
            SEOUL_ZONE
    );

    private MarketSnapshotIngestService marketSnapshotIngestService;
    private MarketSnapshotScheduler scheduler;

    @BeforeEach
    void setUp() {
        marketSnapshotIngestService = mock(MarketSnapshotIngestService.class);
        scheduler = new MarketSnapshotScheduler(marketSnapshotIngestService, "Asia/Seoul", FIXED_CLOCK);
    }

    @Test
    void reingestsFourteenDayRangeFromFourteenDaysAgoToYesterday() {
        when(marketSnapshotIngestService.ingest(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new MarketSnapshotIngestResult(1, 1, 1, 0, 0));

        scheduler.ingestRecentMarketSnapshots();

        LocalDate today = LocalDate.now(FIXED_CLOCK);
        LocalDate startDate = today.minusDays(14);
        LocalDate endDate = today.minusDays(1);

        for (LocalDate targetDate = startDate; !targetDate.isAfter(endDate); targetDate = targetDate.plusDays(1)) {
            verify(marketSnapshotIngestService).ingest(eq(targetDate), eq(targetDate));
        }
        verify(marketSnapshotIngestService, times(14)).ingest(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void continuesRemainingDatesAndDoesNotPropagateWhenOneDateFails() {
        LocalDate failedDate = LocalDate.of(2026, 8, 5);
        when(marketSnapshotIngestService.ingest(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new MarketSnapshotIngestResult(1, 1, 1, 0, 0));
        when(marketSnapshotIngestService.ingest(eq(failedDate), eq(failedDate)))
                .thenThrow(new RuntimeException("parse failed"));

        assertDoesNotThrow(() -> scheduler.ingestRecentMarketSnapshots());

        verify(marketSnapshotIngestService, times(14)).ingest(any(LocalDate.class), any(LocalDate.class));
        verify(marketSnapshotIngestService).ingest(eq(LocalDate.of(2026, 8, 6)), eq(LocalDate.of(2026, 8, 6)));
        verify(marketSnapshotIngestService).ingest(eq(LocalDate.of(2026, 8, 12)), eq(LocalDate.of(2026, 8, 12)));
    }

    @Test
    void doesNotPropagateWhenSchedulerExecutionFailsBeforeIngest() {
        Clock brokenClock = mock(Clock.class);
        when(brokenClock.getZone()).thenReturn(SEOUL_ZONE);
        when(brokenClock.instant()).thenThrow(new RuntimeException("clock unavailable"));
        MarketSnapshotScheduler invalidZoneScheduler =
                new MarketSnapshotScheduler(marketSnapshotIngestService, "Asia/Seoul", brokenClock);

        assertDoesNotThrow(invalidZoneScheduler::ingestRecentMarketSnapshots);
        verifyNoInteractions(marketSnapshotIngestService);
    }
}
