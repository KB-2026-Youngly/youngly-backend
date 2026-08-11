package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.GuardrailResult;
import com.kb.youngly.dto.recommendation.PensionForecastFacts;
import com.kb.youngly.dto.recommendation.YounglyRecommendationResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NumericGuardrailValidator {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("[0-9][0-9,]*\\s?원");

    public GuardrailResult validate(
            YounglyRecommendationResponse response,
            PensionForecastFacts facts
    ) {
        if (response == null) {
            return GuardrailResult.failed("응답 없음");
        }

        Set<String> allowedAmounts = buildAllowedAmounts(facts);

        String combinedText = String.join(" ",
                response.marketHighlights() == null ? "" : String.join(" ", response.marketHighlights()),
                nullToEmpty(response.marketDetail()),
                nullToEmpty(response.pensionInsightIntro()),
                nullToEmpty(response.pensionInsightStrategy())
        );

        Matcher matcher = AMOUNT_PATTERN.matcher(combinedText);
        List<String> unrecognized = new ArrayList<>();

        while (matcher.find()) {
            String found = normalize(matcher.group());
            if (!allowedAmounts.contains(found)) {
                unrecognized.add(found);
            }
        }

        // 예상 적립금은 Java(PensionForecastService)가 계산하는 신뢰 가능한 값이다.
        // LLM 본문에 반드시 포함될 필요는 없으므로 필수 검증에서 제외한다.
        if (!unrecognized.isEmpty()) {
            return GuardrailResult.failed("근거 없는 금액 발견: " + unrecognized);
        }

        return GuardrailResult.success();
    }

    private Set<String> buildAllowedAmounts(PensionForecastFacts facts) {
        Set<String> allowed = new HashSet<>();

        allowed.add(normalize(formatWon(facts.currentPensionBalance())));
        allowed.add(normalize(formatWon(facts.settledAmountThisMonth())));
        allowed.add(normalize(formatWon(facts.expectedAdditionalAmountCurrentRank())));
        allowed.add(normalize(formatWon(facts.expectedAdditionalAmountBestCase())));
        allowed.add(normalize(formatWon(facts.expectedTotalAmountThisMonth())));
        allowed.add(normalize(formatWon(facts.expectedMaxTotalAmountThisMonth())));

        facts.groupContributions().forEach(group -> {
            allowed.add(normalize(formatWon(group.contributionMin())));
            allowed.add(normalize(formatWon(group.contributionMax())));
        });

        return allowed;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s", "");
    }

    private String formatWon(BigDecimal amount) {
        return amount == null ? "" : String.format("%,d원", amount.longValue());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
