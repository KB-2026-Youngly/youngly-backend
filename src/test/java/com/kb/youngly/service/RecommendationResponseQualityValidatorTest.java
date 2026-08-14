package com.kb.youngly.service;

import com.kb.youngly.client.OpenAiHttpClient;
import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.exception.RecommendationQualityValidationException;
import com.kb.youngly.prompt.RecommendationPromptBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationResponseQualityValidatorTest {

    private final RecommendationResponseQualityValidator validator =
            new RecommendationResponseQualityValidator();

    @Test
    void acceptsCompletePersonalizedResponse() {
        RecommendationResponse response = validResponse();

        assertDoesNotThrow(() -> validator.validate(response));
        assertTrue(response.pensionInsightIntro().length() >= 110);
        assertTrue(response.pensionInsightIntro().length() <= 170);
    }

    @Test
    void rejectsInternalTermsBeforeSaving() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                "고객님은 baselineLabel인 적극적인 성장형과 투자관심 분야인 IT/테크, 반도체를 반영해 개인연금 인사이트를 구성합니다. "
                        + "2~3년의 투자 경험과 금융상품의 수익률·위험을 비교할 수 있다는 응답을 바탕으로 장기 자산배분과 분산의 원칙 안에서 관리하는 것이 중요해요.",
                strategy()
        );

        assertThrows(IllegalStateException.class, () -> validator.validate(response));
    }

    @Test
    void rejectsTruncatedIntroWithoutFinalPeriod() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                intro().substring(0, intro().length() - 1),
                strategy()
        );

        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> validator.validate(response)
        );
        assertEquals("ENDING_PERIOD", exception.getFailureType());
        assertEquals("pensionInsightIntro", exception.getFieldName());
        assertEquals("요", exception.getLastChar());
        assertEquals(response.pensionInsightIntro().length(), exception.getLength());
    }

    @Test
    void rejectsIntroAtSchemaMaxLengthWhenFinalPeriodIsMissing() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                "고객님은 " + "가".repeat(275),
                strategy()
        );

        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> validator.validate(response)
        );
        assertEquals(280, response.pensionInsightIntro().length());
        assertEquals("LENGTH_RANGE", exception.getFailureType());
        assertEquals("pensionInsightIntro", exception.getFieldName());
        assertEquals(280, exception.getLength());
    }

    @Test
    void rejectsIntroAtLooseSchemaMaxLengthWhenFinalPeriodIsMissing() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                "고객님은 " + "가".repeat(395),
                strategy()
        );

        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> validator.validate(response)
        );
        assertEquals(400, response.pensionInsightIntro().length());
        assertEquals("LENGTH_RANGE", exception.getFailureType());
        assertEquals("pensionInsightIntro", exception.getFieldName());
        assertEquals(400, exception.getLength());
    }

    @Test
    void rejectsTooShortIntro() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                "고객님은 적극적인 성장형 성향입니다.",
                strategy()
        );

        assertThrows(IllegalStateException.class, () -> validator.validate(response));
    }

    @Test
    void rejectsStrategyWithInvalidEnding() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                intro(),
                "적극적인 성장형 성향을 바탕으로 분산 투자와 꾸준한 적립을 장기 관점에서 이어가는 전략입니다."
        );

        assertThrows(IllegalStateException.class, () -> validator.validate(response));
    }

    @Test
    void acceptsStrategyWithSurveyBasedPensionPrinciple() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                intro(),
                "원금 손실을 피하려는 기준에 맞게 위험이 큰 비중은 없는지 확인하고, 개인연금 계좌의 안정성 기준을 먼저 정해 보세요."
        );

        assertDoesNotThrow(() -> validator.validate(response));
    }

    @Test
    void rejectsRepeatedEndingInStrategy() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                intro(),
                "원금 손실을 피하려는 기준에 맞게 위험이 큰 비중은 없는지 확인하고, 개인연금 계좌의 안정성 기준을 정해 보세요. 보세요."
        );

        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> validator.validate(response)
        );
        assertEquals("REPEATED_ENDING", exception.getFailureType());
    }

    @Test
    void rejectsUnsupportedAutoSavingStrategy() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                intro(),
                "원금 손실을 피하려는 기준에 맞게 위험이 큰 비중은 없는지 확인하고, 개인연금 계좌 자동적립을 시작해 보세요."
        );

        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> validator.validate(response)
        );
        assertEquals("UNSUPPORTED_OR_FORCEFUL_PHRASE", exception.getFailureType());
    }

    @Test
    void rejectsChallengeForecastAndDepositDateInStrategy() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                intro(),
                "이번 달 예상 적립액과 입금 예정일을 진행 중 챌린지 성과와 함께 확인하고 개인연금 점검 기준을 정해 보세요."
        );

        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> validator.validate(response)
        );
        assertEquals("UNSUPPORTED_OR_FORCEFUL_PHRASE", exception.getFailureType());
    }

    @Test
    void treatsUserFacingStyleBannedPhraseAsSoftFailure() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                intro(),
                "투자 성향에 맞춰 연금 계좌의 자산군 비중을 확인하고, 현재 위험 수준이 부담스럽지 않은지 살펴보세요."
        );

        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> validator.validate(response)
        );
        assertEquals("STYLE_WORDING", exception.getFailureType());
        assertEquals("personalInsight", exception.getFieldName());
        assertEquals("투자 성향에 맞춰", exception.getBannedTerm());
        assertFalse(exception.isHardFailure());
    }

    @Test
    void treatsUnfoundedPrincipalProtectionAsSoftStyleFailure() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                "투자 경험이 많지 않고 안정성을 우선한다면, 개인연금은 단기 변동보다 원금 보호를 먼저 생각하며 차분히 살펴보는 방식이 자연스러워요.",
                strategy()
        );

        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> validator.validate(response)
        );
        assertEquals("STYLE_WORDING", exception.getFailureType());
        assertEquals("원금 보호", exception.getBannedTerm());
        assertFalse(exception.isHardFailure());
    }

    @Test
    void rejectsAnalysisTagArtifactAsHardFailure() {
        assertModelArtifact("연금 계좌의 자산군별 비중을 확인해 현재 위험 수준이 부담스럽지 않은지 살펴보세요. </analysis>");
    }

    @Test
    void rejectsCodeFenceArtifactAsHardFailure() {
        assertModelArtifact("연금 계좌의 자산군별 비중을 확인해 현재 위험 수준이 부담스럽지 않은지 살펴보세요. ```");
    }

    @Test
    void rejectsJsonBraceTailArtifactAsHardFailure() {
        assertModelArtifact("연금 계좌의 자산군별 비중을 확인해 현재 위험 수준이 부담스럽지 않은지 살펴보세요. ”} } }");
    }

    @Test
    void rejectsRepeatedNumberArtifactAsHardFailure() {
        assertModelArtifact("연금 계좌의 자산군별 비중을 확인해 현재 위험 수준이 부담스럽지 않은지 살펴보세요. 0 0 0 0 0 0 0 0 0 0");
    }

    @Test
    void rejectsArtifactInMarketDetailAsHardFailure() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail() + " </analysis>",
                intro(),
                strategy()
        );

        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> validator.validate(response)
        );
        assertEquals("MODEL_OUTPUT_ARTIFACT", exception.getFailureType());
        assertEquals("marketDetail", exception.getFieldName());
        assertTrue(exception.isHardFailure());
    }

    @Test
    void rejectsArtifactInMarketHighlightAsHardFailure() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                List.of("미국 지수 상승", "채권 금리 안정 ```", "원달러 횡보", "원자재 혼조"),
                marketDetail(),
                intro(),
                strategy()
        );

        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> validator.validate(response)
        );
        assertEquals("MODEL_OUTPUT_ARTIFACT", exception.getFailureType());
        assertEquals("marketHighlights[1]", exception.getFieldName());
        assertTrue(exception.isHardFailure());
    }

    @Test
    void doesNotRejectNormalKoreanUserSystemWords() {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                "사용자에게 필요한 안내를 차분히 제공하는 시스템이라고 표현해도, 한국어 문장 안에서는 제어 토큰이 아니에요. "
                        + "개인연금은 단기 흐름보다 장기 관리가 중요하므로 위험 수준을 먼저 살펴보세요.",
                strategy()
        );

        assertDoesNotThrow(() -> validator.validate(response));
    }

    @Test
    void lengthContractsAreAlignedAcrossPromptSchemaAndServiceValidator() throws Exception {
        assertEquals(60, staticInt(OpenAiHttpClient.class, "PENSION_INSIGHT_INTRO_MIN_LENGTH"));
        assertEquals(220, staticInt(OpenAiHttpClient.class, "PENSION_INSIGHT_INTRO_MAX_LENGTH"));
        assertEquals(400, staticInt(OpenAiHttpClient.class, "PENSION_INSIGHT_INTRO_SCHEMA_MAX_LENGTH"));
        assertEquals(60, staticInt(OpenAiHttpClient.class, "PENSION_INSIGHT_STRATEGY_MIN_LENGTH"));
        assertEquals(220, staticInt(OpenAiHttpClient.class, "PENSION_INSIGHT_STRATEGY_MAX_LENGTH"));
        assertEquals(200, staticInt(OpenAiHttpClient.class, "PENSION_INSIGHT_STRATEGY_SCHEMA_MAX_LENGTH"));

        assertEquals(60, staticInt(RecommendationResponseQualityValidator.class, "PENSION_INSIGHT_INTRO_MIN_LENGTH"));
        assertEquals(220, staticInt(RecommendationResponseQualityValidator.class, "PENSION_INSIGHT_INTRO_MAX_LENGTH"));
        assertEquals(60, staticInt(RecommendationResponseQualityValidator.class, "PENSION_INSIGHT_STRATEGY_MIN_LENGTH"));
        assertEquals(220, staticInt(RecommendationResponseQualityValidator.class, "PENSION_INSIGHT_STRATEGY_MAX_LENGTH"));

        String systemMessage = staticString(RecommendationPromptBuilder.class, "SYSTEM_MESSAGE");
        assertTrue(systemMessage.contains("챌린지, 성공 횟수, 정산액, 예상 적립액"));
        assertTrue(systemMessage.contains("투자 상품 실행이 아닌 사용자가 확인할 수 있는 행동 하나"));
        assertTrue(systemMessage.contains("서로 다른 행동을 두 개 제안하지 말고"));
        assertTrue(systemMessage.contains("원금 보호"));
        assertTrue(systemMessage.contains("60~220자 내외의 자연스러운 한국어"));
    }

    private static RecommendationResponse validResponse() {
        return RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                intro(),
                strategy()
        );
    }

    private static List<String> highlights() {
        return List.of("미국 지수 상승", "채권 금리 안정", "원달러 횡보", "원자재 혼조");
    }

    private static String marketDetail() {
        return "최근 시장은 주요 지수와 금리 흐름이 교차하며 변동성이 이어지고 있어요. "
                + "개인연금 관점에서는 단기 등락보다 장기 흐름을 차분히 살펴보는 태도가 중요해요.";
    }

    private static String intro() {
        return "2~3년의 투자 경험과 1~3년 계획, 변동성 감수 응답을 보면 적극적인 성장형 성향이 드러나요. "
                + "개인연금은 단기 흐름보다 장기 관리가 중요하므로 IT/테크 관심도 분산 원칙 안에서 점검해 보세요.";
    }

    private static String strategy() {
        return "IT/테크 관심은 분산 원칙으로 나누고 장기 점검 일정을 정해 매달 한 번씩 변동 흐름을 차분히 살펴보세요.";
    }

    private void assertModelArtifact(String strategy) {
        RecommendationResponse response = RecommendationResponse.llmTextOnly(
                highlights(),
                marketDetail(),
                intro(),
                strategy
        );

        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> validator.validate(response)
        );
        assertEquals("MODEL_OUTPUT_ARTIFACT", exception.getFailureType());
        assertEquals("pensionInsightStrategy", exception.getFieldName());
        assertTrue(exception.isHardFailure());
    }

    private static int staticInt(Class<?> targetClass, String fieldName) throws Exception {
        Field field = targetClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(null);
    }

    private static String staticString(Class<?> targetClass, String fieldName) throws Exception {
        Field field = targetClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(null);
    }
}
