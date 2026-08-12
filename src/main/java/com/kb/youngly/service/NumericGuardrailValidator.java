package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.GuardrailResult;
import com.kb.youngly.dto.recommendation.PensionForecastFacts;
import com.kb.youngly.dto.recommendation.RecommendationResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    private static final List<String> BANNED_PHRASES = List.of(
            "추천합니다",
            "사세요",
            "매수하세요",
            "매도하세요",
            "ETF를 선택하세요",
            "비중을 늘리세요",
            "비중을 줄이세요",
            "반드시 해야 합니다",
            "확실히 오를 것입니다",
            "확실히 내릴 것입니다"
    );

    public GuardrailResult validate(
            RecommendationResponse response,
            PensionForecastFacts facts
    ) {
        if (response == null) {
            return GuardrailResult.failed("응답 없음");
        }

        if (response.marketHighlights() == null || response.marketHighlights().isEmpty()) {
            return GuardrailResult.failed("필수 필드 누락: marketHighlights");
        }
        for (String item : response.marketHighlights()) {
            if (!StringUtils.hasText(item)) {
                return GuardrailResult.failed("필수 필드 빈 문자열: marketHighlights");
            }
        }
        if (!StringUtils.hasText(response.marketDetail())) {
            return GuardrailResult.failed("필수 필드 누락: marketDetail");
        }
        if (!StringUtils.hasText(response.pensionInsightIntro())) {
            return GuardrailResult.failed("필수 필드 누락: pensionInsightIntro");
        }
        if (!StringUtils.hasText(response.pensionInsightStrategy())) {
            return GuardrailResult.failed("필수 필드 누락: pensionInsightStrategy");
        }

        String combinedText = String.join(" ",
                String.join(" ", response.marketHighlights()),
                response.marketDetail(),
                response.pensionInsightIntro(),
                response.pensionInsightStrategy()
        );

        for (String bannedPhrase : BANNED_PHRASES) {
            if (combinedText.contains(bannedPhrase)) {
                return GuardrailResult.failed("금지 표현 포함: " + bannedPhrase);
            }
        }

        Set<String> allowedAmounts = buildAllowedAmounts(facts);
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
}
