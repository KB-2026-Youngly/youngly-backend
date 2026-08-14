package com.kb.youngly.config;

import com.kb.youngly.client.MarketSnapshotStartupListener;
import com.kb.youngly.scheduler.MarketSnapshotScheduler;
import com.kb.youngly.service.MarketSnapshotOrchestrationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MarketSnapshotAutoCollectionConditionTest {

    @Test
    void doesNotRegisterStartupOrSchedulerBeansWhenPropertiesAreMissing() {
        try (AnnotationConfigApplicationContext context = contextWithProperties(Map.of())) {
            assertFalse(context.containsBean("marketSnapshotStartupListener"));
            assertFalse(context.containsBean("marketSnapshotScheduler"));
        }
    }

    @Test
    void doesNotRegisterStartupOrSchedulerBeansWhenPropertiesAreFalse() {
        try (AnnotationConfigApplicationContext context = contextWithProperties(Map.of(
                "market.snapshot.startup.enabled", "false",
                "market.snapshot.scheduler.enabled", "false"
        ))) {
            assertFalse(context.containsBean("marketSnapshotStartupListener"));
            assertFalse(context.containsBean("marketSnapshotScheduler"));
        }
    }

    @Test
    void registersStartupBeanOnlyWhenStartupPropertyIsTrue() {
        try (AnnotationConfigApplicationContext context = contextWithProperties(Map.of(
                "market.snapshot.startup.enabled", "true",
                "market.snapshot.scheduler.enabled", "false"
        ))) {
            assertTrue(context.containsBean("marketSnapshotStartupListener"));
            assertFalse(context.containsBean("marketSnapshotScheduler"));
        }
    }

    @Test
    void registersSchedulerBeanOnlyWhenSchedulerPropertyIsTrue() {
        try (AnnotationConfigApplicationContext context = contextWithProperties(Map.of(
                "market.snapshot.startup.enabled", "false",
                "market.snapshot.scheduler.enabled", "true"
        ))) {
            assertFalse(context.containsBean("marketSnapshotStartupListener"));
            assertTrue(context.containsBean("marketSnapshotScheduler"));
        }
    }

    private AnnotationConfigApplicationContext contextWithProperties(Map<String, Object> properties) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("testMarketSnapshotProperties", properties));
        context.register(TestConfig.class);
        context.register(MarketSnapshotStartupListener.class);
        context.register(MarketSnapshotScheduler.class);
        context.refresh();
        return context;
    }

    @Configuration
    static class TestConfig {
        @Bean
        MarketSnapshotOrchestrationService marketSnapshotOrchestrationService() {
            return mock(MarketSnapshotOrchestrationService.class);
        }
    }
}
