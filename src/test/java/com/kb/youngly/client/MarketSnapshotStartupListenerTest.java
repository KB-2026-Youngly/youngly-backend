package com.kb.youngly.client;

import com.kb.youngly.service.MarketSnapshotOrchestrationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class MarketSnapshotStartupListenerTest {

    @Test
    void callsOrchestrationOnceOnServletContextRefresh() {
        MarketSnapshotOrchestrationService orchestrationService =
                mock(MarketSnapshotOrchestrationService.class);

        MarketSnapshotStartupListener listener =
                new MarketSnapshotStartupListener(orchestrationService);

        listener.bootstrap(eventWithParent());
        listener.bootstrap(eventWithParent());

        verify(orchestrationService, times(1))
                .reingestRecentTwoWeeksAndSummarize("STARTUP");
    }

    @Test
    void ignoresRootContextRefresh() {
        MarketSnapshotOrchestrationService orchestrationService =
                mock(MarketSnapshotOrchestrationService.class);

        MarketSnapshotStartupListener listener =
                new MarketSnapshotStartupListener(orchestrationService);

        listener.bootstrap(rootEvent());

        verifyNoInteractions(orchestrationService);
    }

    @Test
    void doesNotPropagateWhenStartupOrchestrationFails() {
        MarketSnapshotOrchestrationService orchestrationService =
                mock(MarketSnapshotOrchestrationService.class);

        when(orchestrationService.reingestRecentTwoWeeksAndSummarize("STARTUP"))
                .thenThrow(new RuntimeException("fss unavailable"));

        MarketSnapshotStartupListener listener =
                new MarketSnapshotStartupListener(orchestrationService);

        assertDoesNotThrow(() -> listener.bootstrap(eventWithParent()));
    }

    private ContextRefreshedEvent eventWithParent() {
        ApplicationContext parent = mock(ApplicationContext.class);
        ApplicationContext child = mock(ApplicationContext.class);
        when(child.getParent()).thenReturn(parent);
        return new ContextRefreshedEvent(child);
    }

    private ContextRefreshedEvent rootEvent() {
        ApplicationContext root = mock(ApplicationContext.class);
        when(root.getParent()).thenReturn(null);
        return new ContextRefreshedEvent(root);
    }
}