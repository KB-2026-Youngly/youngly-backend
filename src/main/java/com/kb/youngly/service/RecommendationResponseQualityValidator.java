package com.kb.youngly.service;

import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.exception.RecommendationQualityValidationException;
import com.kb.youngly.util.ModelOutputArtifactValidator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class RecommendationResponseQualityValidator {

    private static final int PENSION_INSIGHT_INTRO_MIN_LENGTH = 60;
    private static final int PENSION_INSIGHT_INTRO_MAX_LENGTH = 220;
    private static final int PENSION_INSIGHT_STRATEGY_MIN_LENGTH = 60;
    private static final int PENSION_INSIGHT_STRATEGY_MAX_LENGTH = 220;
    private static final Pattern SAME_EOJEOL_REPEAT = Pattern.compile("(^|\\s)(\\S+)(\\s+\\2)(\\s|$)");
    private static final Pattern SAME_SENTENCE_REPEAT = Pattern.compile("([^.!?。]+[.!?。])\\s*\\1");

    private static final List<String> INTERNAL_BANNED_TERMS = List.of(
            "baseline",
            "baselineLabel",
            "investmentInterests",
            "hobbyInterests",
            "surveyQuestionAnswers",
            "marketContext",
            "FACTS",
            "JSON",
            "DTO",
            "프롬프트",
            "최적화",
            "인사이트를 구성합니다",
            "반영해 구성합니다",
            "작성합니다",
            "인사이트를"
    );

    private static final List<String> STRATEGY_ALLOWED_ENDINGS = List.of(
            "하세요.",
            "보세요."
    );

    private static final List<String> PROMPT_LEAK_BANNED_PHRASES = List.of(
            "입력 데이터",
            "데이터를 기반으로",
            "입력값을 반영",
            "필드를",
            "분산 2개",
            "자연스럽게 포함",
            "한 가지로 좁히되",
            "지정 종결어",
            "두 문장으로 작성",
            "한 문장으로 작성",
            "글자 이내",
            "글자 이상",
            "목표 길이",
            "작성 규칙",
            "출력 규칙",
            "실제 사용자 사실",
            "최소 하나",
            "최대 하나",
            "정확히 2개"
    );

    private static final List<String> STYLE_BANNED_PHRASES = List.of(
            "당신은",
            "투자 성향에 맞춰",
            "투자성향에 맞춰",
            "연금 운용 원칙으로",
            "정보 탐색이 필요할 수 있습니다",
            "스스로의 감당력",
            "권합니다",
            "도움이 됩니다",
            "조건이 허용된다면",
            "원금 보호",
            "비상금",
            "비상 상황 대비",
            "단기 생활비",
            "장기보다는 단기 목표",
            "원금 보장",
            "연금 설계",
            "수익과 변동성의 관계를 체크",
            "점이 반영됩니다",
            "해당 분야",
            "금융 이해도 기초",
            "실제로 관심 있는 분야를 한 가지로 좁히되"
    );

    private static final List<String> UNSUPPORTED_OR_FORCEFUL_PHRASES = List.of(
            "자동적립",
            "자동 적립",
            "자동투자",
            "자동 투자",
            "자동매수",
            "자동 매수",
            "자동 납입",
            "지금 바로",
            "반드시",
            "확실히",
            "보장",
            "챌린지",
            "성공 횟수",
            "정산액",
            "예상 적립",
            "예상 적립액",
            "최대 적립",
            "최대 적립액",
            "입금 예정일",
            "계좌 잔액",
            "이번 달 예상",
            "다음 점검 기준을 정해 보세요"
    );

    private static final List<String> BANNED_FINANCIAL_ADVICE_PHRASES = List.of(
            "매수하세요",
            "매도하세요",
            "가입하세요",
            "ETF를 매수",
            "ETF를 선택",
            "펀드를 매수",
            "수익이 보장",
            "수익을 보장",
            "확정 수익",
            "확실히 오를",
            "확실히 내릴"
    );

    public void validate(RecommendationResponse response) {
        validate(response, null);
    }

    public void validate(RecommendationResponse response, Object ignoredFacts) {
        if (response == null) {
            throw new RecommendationQualityValidationException(
                    "추천 응답이 비어 있습니다.",
                    "EMPTY_RESPONSE",
                    null,
                    null,
                    null,
                    null,
                    true
            );
        }

        validateRange("pensionInsightIntro", response.pensionInsightIntro(),
                PENSION_INSIGHT_INTRO_MIN_LENGTH, PENSION_INSIGHT_INTRO_MAX_LENGTH);
        validateEndsWithPeriod("pensionInsightIntro", response.pensionInsightIntro());
        validateOneOrTwoSentences(response.pensionInsightIntro());

        validateRange("pensionInsightStrategy", response.pensionInsightStrategy(),
                PENSION_INSIGHT_STRATEGY_MIN_LENGTH, PENSION_INSIGHT_STRATEGY_MAX_LENGTH);
        ModelOutputArtifactValidator.validateRecommendationResponse(response);
        validateStrategyEnding(response.pensionInsightStrategy());
        validateNoRepeatedText(response.pensionInsightIntro(), "pensionInsightIntro");
        validateNoRepeatedText(response.pensionInsightStrategy(), "pensionInsightStrategy");
        validateNoUnsupportedOrForcefulPhrases(response.pensionInsightStrategy());

        validateNoInternalTerms(String.join("\n",
                response.marketHighlights() == null ? "" : String.join("\n", response.marketHighlights()),
                nullToEmpty(response.marketDetail()),
                nullToEmpty(response.pensionInsightIntro()),
                nullToEmpty(response.pensionInsightStrategy())
        ));
        validateNoPromptLeak(String.join("\n",
                nullToEmpty(response.pensionInsightIntro()),
                nullToEmpty(response.pensionInsightStrategy())
        ));
        validateNoStyleBannedPhrases(String.join("\n",
                nullToEmpty(response.pensionInsightIntro()),
                nullToEmpty(response.pensionInsightStrategy())
        ));
        validateNoBannedFinancialAdvice(String.join("\n",
                nullToEmpty(response.pensionInsightIntro()),
                nullToEmpty(response.pensionInsightStrategy())
        ));
    }

    private void validateRange(String fieldName, String value, int minLength, int maxLength) {
        if (value == null) {
            throw new RecommendationQualityValidationException(
                    "추천 응답 필수 필드가 null입니다. field=" + fieldName,
                    "NULL_FIELD",
                    fieldName,
                    null,
                    null,
                    null,
                    true
            );
        }
        if (value.length() < minLength || value.length() > maxLength) {
            throw new RecommendationQualityValidationException(
                    "추천 응답 길이 범위를 벗어났습니다. field=" + fieldName
                            + ", length=" + value.length()
                            + ", minLength=" + minLength
                            + ", maxLength=" + maxLength,
                    "LENGTH_RANGE",
                    fieldName,
                    value.length(),
                    lastChar(value),
                    null,
                    false
            );
        }
    }

    private void validateEndsWithPeriod(String fieldName, String value) {
        if (!value.trim().endsWith(".")) {
            throw new RecommendationQualityValidationException(
                    "추천 응답이 완결된 문장으로 끝나지 않습니다. field=" + fieldName,
                    "ENDING_PERIOD",
                    fieldName,
                    value.length(),
                    lastChar(value),
                    null,
                    false
            );
        }
    }

    private void validateStrategyEnding(String value) {
        String trimmed = value.trim();
        boolean valid = STRATEGY_ALLOWED_ENDINGS.stream().anyMatch(trimmed::endsWith);
        if (!valid) {
            throw new RecommendationQualityValidationException(
                    "추천 응답 전략 문장 종결어가 올바르지 않습니다.",
                    "STRATEGY_ENDING",
                    "pensionInsightStrategy",
                    value.length(),
                    lastChar(value),
                    null,
                    false
            );
        }
    }

    private void validateOneOrTwoSentences(String value) {
        String trimmed = value == null ? "" : value.trim();
        int sentenceCount = countSentenceEndingPeriods(trimmed);
        if (sentenceCount < 1 || sentenceCount > 2) {
            throw new RecommendationQualityValidationException(
                "추천 응답 intro 문장 수가 올바르지 않습니다. expected=1~2, actual=" + sentenceCount,
                    "INTRO_SENTENCE_COUNT",
                    "pensionInsightIntro",
                    value.length(),
                    lastChar(value),
                    null,
                    false
            );
        }
    }

    private int countSentenceEndingPeriods(String value) {
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) != '.') {
                continue;
            }

            if (index == value.length() - 1 || Character.isWhitespace(value.charAt(index + 1))) {
                count++;
            }
        }
        return count;
    }

    private void validateNoInternalTerms(String combined) {
        for (String term : INTERNAL_BANNED_TERMS) {
            if (combined.contains(term)) {
                throw new RecommendationQualityValidationException(
                        "추천 응답에 내부 용어가 포함되어 있습니다. term=" + term,
                        "BANNED_INTERNAL_TERM",
                        "combinedText",
                        combined.length(),
                        null,
                        term,
                        true
                );
            }
        }
    }

    private void validateNoPromptLeak(String personalInsight) {
        for (String phrase : PROMPT_LEAK_BANNED_PHRASES) {
            if (personalInsight.contains(phrase)) {
                throw new RecommendationQualityValidationException(
                        "추천 응답에 프롬프트 지시 문구가 포함되어 있습니다. phrase=" + phrase,
                        "PROMPT_LEAK",
                        "personalInsight",
                        personalInsight.length(),
                        null,
                        phrase,
                        true
                );
            }
        }
    }

    private void validateNoStyleBannedPhrases(String personalInsight) {
        for (String phrase : STYLE_BANNED_PHRASES) {
            if (personalInsight.contains(phrase)) {
                throw new RecommendationQualityValidationException(
                        "추천 응답에 사용자 노출 문체 금지 표현이 포함되어 있습니다. phrase=" + phrase,
                        "STYLE_WORDING",
                        "personalInsight",
                        personalInsight.length(),
                        null,
                        phrase,
                        false
                );
            }
        }
    }

    private void validateNoRepeatedText(String value, String fieldName) {
        String text = nullToEmpty(value).trim();
        if (SAME_EOJEOL_REPEAT.matcher(text).find()) {
            throw new RecommendationQualityValidationException(
                    "추천 응답에 동일 어절이 연속 반복되었습니다.",
                    "REPEATED_WORD",
                    fieldName,
                    text.length(),
                    lastChar(text),
                    null,
                    false
            );
        }
        if (SAME_SENTENCE_REPEAT.matcher(text).find()
                || text.contains("보세요. 보세요.")
                || text.contains("하세요. 하세요.")) {
            throw new RecommendationQualityValidationException(
                    "추천 응답에 동일 문장 또는 종결 표현이 반복되었습니다.",
                    "REPEATED_ENDING",
                    fieldName,
                    text.length(),
                    lastChar(text),
                    null,
                    true
            );
        }
    }

    private void validateNoUnsupportedOrForcefulPhrases(String strategy) {
        String text = nullToEmpty(strategy);
        for (String phrase : UNSUPPORTED_OR_FORCEFUL_PHRASES) {
            if (text.contains(phrase)) {
                throw new RecommendationQualityValidationException(
                        "추천 전략에 미지원 기능 또는 과도한 권유 표현이 포함되어 있습니다.",
                        "UNSUPPORTED_OR_FORCEFUL_PHRASE",
                        "pensionInsightStrategy",
                        text.length(),
                        lastChar(text),
                        phrase,
                        true
                );
            }
        }
    }

    private void validateNoBannedFinancialAdvice(String personalInsight) {
        for (String phrase : BANNED_FINANCIAL_ADVICE_PHRASES) {
            if (personalInsight.contains(phrase)) {
                throw new RecommendationQualityValidationException(
                        "추천 응답에 금융 안전 금지 표현이 포함되어 있습니다.",
                        "BANNED_FINANCIAL_ADVICE",
                        "personalInsight",
                        personalInsight.length(),
                        null,
                        phrase,
                        true
                );
            }
        }
    }

    private String lastChar(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? "" : trimmed.substring(trimmed.length() - 1);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
