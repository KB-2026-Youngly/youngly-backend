package com.kb.youngly.dto.recommendation;

import com.kb.youngly.enums.PersonalInsightStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PensionInsightPageResponse(
        Forecast forecast,
        MarketSummary marketSummary,
        UserInterests userInterests,
        PersonalInsightStatus personalInsightStatus,
        PersonalInsight personalInsight
) {
    public record Forecast(
            BigDecimal currentBalance,
            BigDecimal expectedMinAmount,
            BigDecimal expectedAmount,
            BigDecimal expectedMaxAmount,
            LocalDate nextDepositDate
    ) {
    }

    public record MarketSummary(
            String headline,
            String detail
    ) {
    }

    public record UserInterests(
            List<String> investment,
            List<String> general
    ) {
    }

    public record PersonalInsight(
            String intro,
            String strategy
    ) {
    }
}
