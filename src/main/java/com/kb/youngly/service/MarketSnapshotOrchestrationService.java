package com.kb.youngly.service;

import com.kb.youngly.dto.market.MarketSnapshotIngestResult;
import com.kb.youngly.exception.FssMarketRateLimitException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import com.kb.youngly.util.YounglyTime;

@Service
@Log4j2
public class MarketSnapshotOrchestrationService {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private final MarketSnapshotIngestService marketSnapshotIngestService;
    private final MarketSnapshotSummaryService marketSnapshotSummaryService;
    private final Clock clock;

    @Autowired
    public MarketSnapshotOrchestrationService(
            MarketSnapshotIngestService marketSnapshotIngestService,
            MarketSnapshotSummaryService marketSnapshotSummaryService
    ) {
        this(
                marketSnapshotIngestService,
                marketSnapshotSummaryService,
                YounglyTime.clock()
        );
    }

    MarketSnapshotOrchestrationService(
            MarketSnapshotIngestService marketSnapshotIngestService,
            MarketSnapshotSummaryService marketSnapshotSummaryService,
            Clock clock
    ) {
        this.marketSnapshotIngestService = marketSnapshotIngestService;
        this.marketSnapshotSummaryService = marketSnapshotSummaryService;
        this.clock = clock;
    }

    public synchronized MarketSnapshotIngestResult reingestRecentTwoWeeksAndSummarize(
            String trigger
    ) {
        LocalDate today = LocalDate.now(clock);

        // 오늘 제외, 어제까지 포함한 최근 14개 날짜
        LocalDate startDate = today.minusDays(14);
        LocalDate endDate = today.minusDays(1);

        MarketSnapshotIngestResult result =
                new MarketSnapshotIngestResult(0, 0, 0, 0, 0);

        try {
            result = marketSnapshotIngestService.ingest(startDate, endDate);
        } catch (FssMarketRateLimitException exception) {
            log.warn(
                    "[MARKET_SNAPSHOT_RATE_LIMIT_REACHED] trigger={}, startDate={}, endDate={}, resultCode={}",
                    trigger,
                    startDate,
                    endDate,
                    FssMarketRateLimitException.RESULT_CODE
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "[MARKET_SNAPSHOT_REINGEST_FAILED] trigger={}, startDate={}, endDate={}",
                    trigger,
                    startDate,
                    endDate,
                    exception
            );
        }

        try {
            marketSnapshotSummaryService.generateLatestRecentSummaryIfAbsent(
                    startDate,
                    endDate
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "[MARKET_SUMMARY_GENERATION_FAILED] "
                            + "trigger={}, startDate={}, endDate={}",
                    trigger,
                    startDate,
                    endDate,
                    exception
            );
        }

        log.info(
                "[MARKET_SNAPSHOT_ORCHESTRATION_DONE] "
                        + "trigger={}, startDate={}, endDate={}, totalItems={}, "
                        + "targetPdfCount={}, savedCount={}, skippedCount={}, failedCount={}",
                trigger,
                startDate,
                endDate,
                result.totalItems(),
                result.targetPdfCount(),
                result.savedCount(),
                result.skippedCount(),
                result.failedCount()
        );

        return result;
    }
}
