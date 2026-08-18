package com.kb.youngly.dto.recommendation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PensionForecastFacts(
        String userId,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        BigDecimal settledAmountThisMonth,
        BigDecimal expectedAdditionalAmountConservative,
        BigDecimal expectedAdditionalAmountCurrentRank,
        BigDecimal expectedAdditionalAmountBestCase,
        BigDecimal expectedTotalAmountThisMonth,
        BigDecimal expectedMaxTotalAmountThisMonth,
        BigDecimal currentPensionBalance,
        boolean hasOngoingRoundThisMonth,
        LocalDate nextDepositDate,
        List<GroupContribution> groupContributions
) {
    public PensionForecastFacts(
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
            List<GroupContribution> groupContributions
    ) {
        this(
                userId, periodStartDate, periodEndDate, settledAmountThisMonth,
                BigDecimal.ZERO, expectedAdditionalAmountCurrentRank,
                expectedAdditionalAmountBestCase, expectedTotalAmountThisMonth,
                expectedMaxTotalAmountThisMonth, currentPensionBalance,
                hasOngoingRoundThisMonth, nextDepositDate, groupContributions
        );
    }
    /**
     * 챌린지별 예상 적립 근거.
     * weeklySuccessRate = successCount / 결산 완료 주차 수 (0이면 0).
     * provisionalRank = success_count 기준 RANK() (updateRoundRanks와 동일).
     */
    public record GroupContribution(
            String groupName,
            String challengeType,
            Integer successCount,
            Integer provisionalRank,
            Double weeklySuccessRate,
            BigDecimal contributionMin,
            BigDecimal contributionMax,
            LocalDate roundEndDate
    ) {
    }
}
