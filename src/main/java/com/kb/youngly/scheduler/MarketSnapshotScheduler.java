package com.kb.youngly.scheduler;

import com.kb.youngly.config.ConditionalOnProperty;
import com.kb.youngly.service.MarketSnapshotOrchestrationService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
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
@ConditionalOnProperty(
        name = "market.snapshot.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@Log4j2
public class MarketSnapshotScheduler {

    private static final String CRON = "0 0 19 * * *";
    private static final String ZONE_ID = "Asia/Seoul";

    private final MarketSnapshotOrchestrationService marketSnapshotOrchestrationService;

    @Autowired
    public MarketSnapshotScheduler(MarketSnapshotOrchestrationService marketSnapshotOrchestrationService) {
        this.marketSnapshotOrchestrationService = marketSnapshotOrchestrationService;
    }

    /**
     * 매일 한국시간 19:00에 전날부터 14일 전까지 시장 동향을 날짜별로 재수집한다.
     */
    @Scheduled(cron = CRON, zone = ZONE_ID)
    public void ingestRecentMarketSnapshots() {
        try {
            marketSnapshotOrchestrationService.reingestRecentTwoWeeksAndSummarize("SCHEDULER");
        } catch (RuntimeException exception) {
            // 실패해도 기존 저장 데이터를 삭제·초기화하지 않는다.
            log.error("시장 동향 스냅샷 스케줄러 실행 실패", exception);
        }
    }
}
