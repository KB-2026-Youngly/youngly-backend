package com.kb.youngly.dto.recommendation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kb.youngly.enums.GenerationMode;
import com.kb.youngly.enums.GuardrailStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RecommendationResponse(
        String userId,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        BigDecimal settledAmountThisMonth,
        BigDecimal expectedAdditionalAmountCurrentRank,
        BigDecimal expectedAdditionalAmountBestCase,
        BigDecimal expectedTotalAmountThisMonth,
        BigDecimal expectedMaxTotalAmountThisMonth,
        BigDecimal currentPensionBalance,
        boolean hasOngoingRoundThisMonth,
        LocalDate nextDepositDate,
        List<PensionForecastFacts.GroupContribution> forecastBasis,
        List<String> marketHighlights,
        String marketDetail,
        String pensionInsightIntro,
        String pensionInsightStrategy,
        @JsonIgnore GenerationMode generationMode,
        @JsonIgnore GuardrailStatus guardrailStatus
) {
    public static RecommendationResponse llmTextOnly(
            List<String> marketHighlights,
            String marketDetail,
            String pensionInsightIntro,
            String pensionInsightStrategy
    ) {
        return new RecommendationResponse(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                marketHighlights,
                marketDetail,
                pensionInsightIntro,
                pensionInsightStrategy,
                null,
                null
        );
    }

    public RecommendationResponse withFacts(
            PensionForecastFacts facts,
            GenerationMode generationMode,
            GuardrailStatus guardrailStatus
    ) {
        return new RecommendationResponse(
                facts.userId(),
                facts.periodStartDate(),
                facts.periodEndDate(),
                facts.settledAmountThisMonth(),
                facts.expectedAdditionalAmountCurrentRank(),
                facts.expectedAdditionalAmountBestCase(),
                facts.expectedTotalAmountThisMonth(),
                facts.expectedMaxTotalAmountThisMonth(),
                facts.currentPensionBalance(),
                facts.hasOngoingRoundThisMonth(),
                facts.nextDepositDate(),
                facts.groupContributions(),
                marketHighlights,
                marketDetail,
                pensionInsightIntro,
                pensionInsightStrategy,
                generationMode,
                guardrailStatus
        );
    }

    /** API 응답 전용. DB에 저장된 generationMode는 변경하지 않는다. */
    public RecommendationResponse withGenerationMode(GenerationMode generationMode) {
        return new RecommendationResponse(
                userId,
                periodStartDate,
                periodEndDate,
                settledAmountThisMonth,
                expectedAdditionalAmountCurrentRank,
                expectedAdditionalAmountBestCase,
                expectedTotalAmountThisMonth,
                expectedMaxTotalAmountThisMonth,
                currentPensionBalance,
                hasOngoingRoundThisMonth,
                nextDepositDate,
                forecastBasis,
                marketHighlights,
                marketDetail,
                pensionInsightIntro,
                pensionInsightStrategy,
                generationMode,
                guardrailStatus
        );
    }
}
