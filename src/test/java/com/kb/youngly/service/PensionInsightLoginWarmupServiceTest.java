package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.enums.Baseline;
import com.kb.youngly.enums.GenerationMode;
import com.kb.youngly.enums.GuardrailStatus;
import com.kb.youngly.mapper.RecommendationMapper;
import com.kb.youngly.vo.user.RecommendationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class PensionInsightLoginWarmupServiceTest {

    private RecommendationMapper recommendationMapper;
    private RecommendationProvider recommendationProvider;
    private PensionInsightLoginWarmupService service;

    @BeforeEach
    void setUp() {
        recommendationMapper = mock(RecommendationMapper.class);
        recommendationProvider = mock(RecommendationProvider.class);
        service = new PensionInsightLoginWarmupService(
                recommendationMapper,
                recommendationProvider
        );
    }

    @Test
    void skipsWhenSameDayLivePassedCacheExists() {
        when(recommendationMapper.selectLatestByUserId("user01"))
                .thenReturn(livePassedVo("user01", LocalDateTime.now()));

        service.warmUpAfterLogin("user01");

        verify(recommendationProvider, never()).provide(anyString());
    }

    @Test
    void generatesInsightWhenCacheMissing() {
        when(recommendationMapper.selectLatestByUserId("user01")).thenReturn(null);
        when(recommendationProvider.provide("user01"))
                .thenReturn(RecommendationResponse.llmTextOnly(
                        List.of("a", "b", "c", "d"), "detail", "intro", "strategy"));

        service.warmUpAfterLogin("user01");

        verify(recommendationProvider).provide("user01");
    }

    @Test
    void doesNotPropagateExceptionWhenGenerationFails() {
        when(recommendationMapper.selectLatestByUserId("user01")).thenReturn(null);
        when(recommendationProvider.provide("user01"))
                .thenThrow(new RuntimeException("openai down"));

        assertDoesNotThrow(() -> service.warmUpAfterLogin("user01"));
        verify(recommendationProvider).provide("user01");
    }

    private static RecommendationVO livePassedVo(String userId, LocalDateTime createdAt) {
        return RecommendationVO.builder()
                .recommendationId(1L)
                .userId(userId)
                .baseline(Baseline.AGGRESSIVE)
                .periodStartDate(LocalDate.of(2026, 8, 1))
                .periodEndDate(LocalDate.of(2026, 8, 31))
                .settledAmountThisMonth(new BigDecimal("30000.00"))
                .expectedAdditionalAmountCurrentRank(new BigDecimal("150000.00"))
                .expectedAdditionalAmountBestCase(new BigDecimal("250000.00"))
                .expectedTotalAmountThisMonth(new BigDecimal("180000.00"))
                .expectedMaxTotalAmountThisMonth(new BigDecimal("280000.00"))
                .currentPensionBalance(new BigDecimal("5030000.00"))
                .hasOngoingRoundThisMonth(true)
                .nextDepositDate(LocalDate.of(2026, 8, 10))
                .forecastBasisJson("[]")
                .marketHighlightsJson("[\"a\",\"b\",\"c\",\"d\"]")
                .marketDetail("시장 상세")
                .pensionInsightIntro("인사이트")
                .pensionInsightStrategy("전략 문장")
                .generationMode(GenerationMode.LIVE)
                .guardrailStatus(GuardrailStatus.PASSED)
                .createdAt(createdAt)
                .build();
    }
}
