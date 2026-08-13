package com.kb.youngly.scheduler;

import com.kb.youngly.service.MarketSnapshotIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 공용(전역) 시장 동향 스냅샷을 주기적으로 수집·저장하는 스케줄러.
 *
 * <p>사용자별이 아니라 서비스 전역으로 1회 수집한다.
 * 기존 {@link MarketSnapshotIngestService}를 재사용하며,
 * 수집 실패 시 예외를 삼키고 이미 저장된 최신 데이터는 유지한다.</p>
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class MarketSnapshotScheduler {

    private final MarketSnapshotIngestService marketSnapshotIngestService;

    /**
     * 설정된 cron(기본: 매일 한국시간 06:00)에 최근 2주 시장 동향을 수집한다.
     *
     * <p>cron은 {@code scheduler.market-snapshot.cron} 설정값으로 분리한다.</p>
     */
    @Scheduled(
            cron = "${scheduler.market-snapshot.cron:0 0 6 * * *}",
            zone = "Asia/Seoul"
    )
    public void ingestRecentMarketSnapshots() {
        try {
            var result = marketSnapshotIngestService.ingestRecentTwoWeeks();
            log.info(
                    "시장 동향 스냅샷 수집 완료: totalItems={}, targetPdfCount={}, savedCount={}, "
                            + "skippedCount={}, failedCount={}",
                    result.totalItems(),
                    result.targetPdfCount(),
                    result.savedCount(),
                    result.skippedCount(),
                    result.failedCount()
            );
        } catch (RuntimeException exception) {
            // 실패해도 기존 저장 데이터를 삭제·초기화하지 않는다.
            log.error("시장 동향 스냅샷 수집 실패", exception);
        }
    }
}
