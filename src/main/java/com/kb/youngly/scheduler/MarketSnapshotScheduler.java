package com.kb.youngly.scheduler;

import com.kb.youngly.dto.market.MarketSnapshotIngestResult;
import com.kb.youngly.service.MarketSnapshotIngestService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 공용(전역) 시장 동향 스냅샷을 주기적으로 수집·저장하는 스케줄러.
 *
 * <p>사용자별이 아니라 서비스 전역으로 1회 수집한다.
 * 기존 {@link MarketSnapshotIngestService}를 재사용하며,
 * 수집 실패 시 예외를 삼키고 이미 저장된 최신 데이터는 유지한다.</p>
 */
@Component
@Log4j2
public class MarketSnapshotScheduler {

    private static final String CRON = "0 0 19 * * *";
    private static final String ZONE_ID = "Asia/Seoul";

    private final MarketSnapshotIngestService marketSnapshotIngestService;
    private final String schedulerZone;
    private final Clock clock;

    @Autowired
    public MarketSnapshotScheduler(MarketSnapshotIngestService marketSnapshotIngestService) {
        this(marketSnapshotIngestService, ZONE_ID, Clock.system(ZoneId.of(ZONE_ID)));
    }

    MarketSnapshotScheduler(MarketSnapshotIngestService marketSnapshotIngestService,
                            String schedulerZone,
                            Clock clock) {
        this.marketSnapshotIngestService = marketSnapshotIngestService;
        this.schedulerZone = schedulerZone;
        this.clock = clock;
    }

    /**
     * 설정된 cron(기본: 매일 한국시간 19:00)에 전날부터 14일 전까지 시장 동향을 날짜별로 재수집한다.
     *
     * <p>cron과 zone은 설정값으로 분리한다.</p>
     */
    @Scheduled(cron = CRON, zone = ZONE_ID)
    public void ingestRecentMarketSnapshots() {
        try {
            LocalDate today = LocalDate.now(clock);
            LocalDate startDate = today.minusDays(14);
            LocalDate endDate = today.minusDays(1);

            MarketSnapshotIngestResult total = new MarketSnapshotIngestResult(0, 0, 0, 0, 0);
            for (LocalDate targetDate = startDate; !targetDate.isAfter(endDate); targetDate = targetDate.plusDays(1)) {
                try {
                    MarketSnapshotIngestResult dailyResult = marketSnapshotIngestService.ingest(targetDate, targetDate);
                    total = merge(total, dailyResult);
                } catch (RuntimeException exception) {
                    log.warn("시장 동향 스냅샷 날짜별 재수집 실패. marketDate={}", targetDate, exception);
                }
            }

            log.info(
                    "시장 동향 스냅샷 재수집 완료: startDate={}, endDate={}, zone={}, totalItems={}, "
                            + "targetPdfCount={}, savedCount={}, skippedCount={}, failedCount={}",
                    startDate,
                    endDate,
                    schedulerZone,
                    total.totalItems(),
                    total.targetPdfCount(),
                    total.savedCount(),
                    total.skippedCount(),
                    total.failedCount()
            );
        } catch (RuntimeException exception) {
            // 실패해도 기존 저장 데이터를 삭제·초기화하지 않는다.
            log.error("시장 동향 스냅샷 스케줄러 실행 실패", exception);
        }
    }

    private MarketSnapshotIngestResult merge(MarketSnapshotIngestResult left, MarketSnapshotIngestResult right) {
        return new MarketSnapshotIngestResult(
                left.totalItems() + right.totalItems(),
                left.targetPdfCount() + right.targetPdfCount(),
                left.savedCount() + right.savedCount(),
                left.skippedCount() + right.skippedCount(),
                left.failedCount() + right.failedCount()
        );
    }
}
