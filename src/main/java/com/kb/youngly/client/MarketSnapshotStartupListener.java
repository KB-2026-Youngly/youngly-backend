package com.kb.youngly.client;

import com.kb.youngly.config.ConditionalOnProperty;
import com.kb.youngly.service.MarketSnapshotOrchestrationService;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(
        name = "market.snapshot.startup.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@Log4j2
public class MarketSnapshotStartupListener {

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final MarketSnapshotOrchestrationService marketSnapshotOrchestrationService;

    public MarketSnapshotStartupListener(MarketSnapshotOrchestrationService marketSnapshotOrchestrationService) {
        this.marketSnapshotOrchestrationService = marketSnapshotOrchestrationService;
    }

    @Async
    @EventListener(ContextRefreshedEvent.class)
    public void bootstrap(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            return;
        }
        if (!started.compareAndSet(false, true)) {
            return;
        }

        try {
            log.info("[MARKET_SNAPSHOT_STARTUP_START]");
            marketSnapshotOrchestrationService.reingestRecentTwoWeeksAndSummarize("STARTUP");
            log.info("[MARKET_SNAPSHOT_STARTUP_DONE]");
        } catch (RuntimeException exception) {
            log.warn("[MARKET_SNAPSHOT_STARTUP_FAILED]", exception);
        }
    }
}
