package com.kb.youngly.client;

import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.exception.RecommendationQualityValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiHttpClientTest {

    private final OpenAiHttpClient client = new OpenAiHttpClient();

    @Test
    void parsesFirstCompleteJsonObjectAndIgnoresTrailingAnalysisArtifact() {
        RecommendationResponse response = client.parseRecommendationOutputText(
                validJson(validIntro(), validStrategy()) + "</analysis> 0 0 0 0 0 0 0 0 0 0"
        );

        assertEquals(validIntro(), response.pensionInsightIntro());
        assertEquals(validStrategy(), response.pensionInsightStrategy());
    }

    @Test
    void parsesFirstCompleteJsonObjectAndIgnoresTrailingCodeFenceArtifact() {
        RecommendationResponse response = client.parseRecommendationOutputText(
                validJson(validIntro(), validStrategy()) + "```"
        );

        assertEquals(validStrategy(), response.pensionInsightStrategy());
    }

    @Test
    void rejectsAnalysisArtifactInsideJsonString() {
        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> client.parseRecommendationOutputText(
                        validJson(validIntro(), validStrategy() + " </analysis>")
                )
        );

        assertEquals("MODEL_OUTPUT_ARTIFACT", exception.getFailureType());
        assertEquals("pensionInsightStrategy", exception.getFieldName());
        assertTrue(exception.isHardFailure());
    }

    @Test
    void rejectsRepeatedNumberArtifactInsideJsonString() {
        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> client.parseRecommendationOutputText(
                        validJson(validIntro(), validStrategy() + " 0 0 0 0 0 0 0 0 0 0")
                )
        );

        assertEquals("MODEL_OUTPUT_ARTIFACT", exception.getFailureType());
        assertEquals("pensionInsightStrategy", exception.getFieldName());
        assertTrue(exception.isHardFailure());
    }

    @Test
    void extractsJsonWithBracesAndEscapedQuoteInsideString() {
        String intro = "예시 {중괄호}와 \"따옴표\"가 문장 안에 있어도 JSON 경계와 혼동하지 않아요. "
                + "개인연금은 감당 가능한 위험 수준을 먼저 정해 두고 차분히 살펴보는 방식이 자연스러워요.";

        RecommendationResponse response = client.parseRecommendationOutputText(
                validJson(intro, validStrategy()) + "</analysis>"
        );

        assertEquals(intro, response.pensionInsightIntro());
        assertEquals(validStrategy(), response.pensionInsightStrategy());
    }

    @Test
    void rejectsUnclosedJsonObject() {
        RecommendationQualityValidationException exception = assertThrows(
                RecommendationQualityValidationException.class,
                () -> OpenAiHttpClient.extractFirstCompleteJsonObject(
                        "{\"pensionInsightIntro\":\"닫히지 않은 JSON\""
                )
        );

        assertEquals("MODEL_OUTPUT_JSON_EXTRACT_FAILED", exception.getFailureType());
        assertTrue(exception.isHardFailure());
    }

    private static String validJson(String intro, String strategy) {
        return """
                {
                  "marketHighlights": ["미국 지수 상승", "채권 금리 안정", "원달러 횡보", "원자재 혼조"],
                  "marketDetail": "최근 시장은 주요 지수와 금리 흐름이 교차하며 변동성이 이어지고 있어요. 개인연금 관점에서는 단기 등락보다 장기 흐름을 차분히 살펴보는 태도가 중요해요. 시장 흐름은 참고 정보로만 확인하는 편이 안전해요.",
                  "pensionInsightIntro": "%s",
                  "pensionInsightStrategy": "%s"
                }
                """.formatted(escapeJson(intro), escapeJson(strategy));
    }

    private static String validIntro() {
        return "투자 경험이 많지 않고 안정성을 우선한다면, 개인연금은 단기 변동에 반응하기보다 감당 가능한 위험 수준을 먼저 정해 두고 차분히 살펴보는 방식이 자연스러워요.";
    }

    private static String validStrategy() {
        return "연금 계좌의 자산군별 비중을 확인해 현재 위험 수준이 부담스럽지 않은지 살펴보세요.";
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
