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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CachedRecommendationProviderTest {

    private RecommendationMapper recommendationMapper;
    private RecommendationService recommendationService;
    private CachedRecommendationProvider provider;

    @BeforeEach
    void setUp() {
        recommendationMapper = mock(RecommendationMapper.class);
        recommendationService = mock(RecommendationService.class);
        provider = new CachedRecommendationProvider(recommendationMapper, recommendationService);
    }

    @Test
    void returnsCachedModeWithoutCallingOpenAiWhenSameDayLivePassedExists() {
        RecommendationVO stored = livePassedVo("user01", LocalDateTime.now());
        when(recommendationMapper.selectLatestByUserId("user01")).thenReturn(stored);

        RecommendationResponse response = provider.provide("user01");

        assertEquals(GenerationMode.CACHED, response.generationMode());
        assertEquals(GuardrailStatus.PASSED, response.guardrailStatus());
        assertEquals(new BigDecimal("180000.00"), response.expectedTotalAmountThisMonth());
        verify(recommendationService, never()).generateRecommendation(anyString());
        verify(recommendationMapper, never()).insert(any());

        // DB 원본 LIVE 값은 변경하지 않는다.
        assertEquals(GenerationMode.LIVE, stored.getGenerationMode());
    }

    @Test
    void doesNotReuseSafeDefaultAsCache() {
        RecommendationVO stored = RecommendationVO.builder()
                .userId("user01")
                .baseline(Baseline.AGGRESSIVE)
                .generationMode(GenerationMode.SAFE_DEFAULT)
                .guardrailStatus(GuardrailStatus.FAILED_FALLBACK)
                .createdAt(LocalDateTime.now())
                .settledAmountThisMonth(new BigDecimal("30000.00"))
                .build();
        when(recommendationMapper.selectLatestByUserId("user01")).thenReturn(stored);

        RecommendationResponse regenerated = RecommendationResponse.llmTextOnly(
                        List.of("a", "b", "c", "d"), "detail", "intro", "strategy")
                .withFacts(
                        new com.kb.youngly.dto.recommendation.PensionForecastFacts(
                                "user01",
                                LocalDate.of(2026, 8, 1),
                                LocalDate.of(2026, 8, 31),
                                new BigDecimal("30000.00"),
                                new BigDecimal("150000.00"),
                                new BigDecimal("250000.00"),
                                new BigDecimal("180000.00"),
                                new BigDecimal("280000.00"),
                                new BigDecimal("5030000.00"),
                                true,
                                LocalDate.of(2026, 8, 10),
                                List.of()
                        ),
                        GenerationMode.LIVE,
                        GuardrailStatus.PASSED
                );
        when(recommendationService.generateRecommendation("user01")).thenReturn(regenerated);

        RecommendationResponse response = provider.provide("user01");

        assertEquals(GenerationMode.LIVE, response.generationMode());
        verify(recommendationService).generateRecommendation("user01");
    }

    @Test
    void doesNotReuseFailedFallbackAsCache() {
        RecommendationVO stored = RecommendationVO.builder()
                .userId("user01")
                .baseline(Baseline.AGGRESSIVE)
                .generationMode(GenerationMode.LIVE)
                .guardrailStatus(GuardrailStatus.FAILED_FALLBACK)
                .createdAt(LocalDateTime.now())
                .build();
        when(recommendationMapper.selectLatestByUserId("user01")).thenReturn(stored);

        RecommendationResponse live = RecommendationMapper.toResponse(
                livePassedVo("user01", LocalDateTime.now())
        );
        when(recommendationService.generateRecommendation("user01")).thenReturn(live);

        RecommendationResponse response = provider.provide("user01");

        assertEquals(GenerationMode.LIVE, response.generationMode());
        verify(recommendationService).generateRecommendation("user01");
    }

    @Test
    void regeneratesWhenLatestLivePassedIsNotToday() {
        RecommendationVO yesterday = livePassedVo(
                "user01",
                LocalDate.now().minusDays(1).atTime(10, 0)
        );
        when(recommendationMapper.selectLatestByUserId("user01")).thenReturn(yesterday);
        when(recommendationService.generateRecommendation("user01"))
                .thenReturn(RecommendationMapper.toResponse(livePassedVo("user01", LocalDateTime.now())));

        RecommendationResponse response = provider.provide("user01");

        assertEquals(GenerationMode.LIVE, response.generationMode());
        verify(recommendationService).generateRecommendation("user01");
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
