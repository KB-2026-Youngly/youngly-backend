package com.kb.youngly.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.youngly.dto.recommendation.RecommendationPrompt;
import com.kb.youngly.dto.recommendation.YounglyRecommendationResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Lazy
public class OpenAiHttpClient implements OpenAiClient {

    private static final String RESPONSES_API_URL = "https://api.openai.com/v1/responses";

    // marketHighlights
    private static final int MARKET_HIGHLIGHTS_MIN_ITEMS = 4;
    private static final int MARKET_HIGHLIGHTS_MAX_ITEMS = 4;
    private static final int MARKET_HIGHLIGHT_MAX_LENGTH = 30;

    // marketDetail
    private static final int MARKET_DETAIL_MIN_LENGTH = 150;
    private static final int MARKET_DETAIL_MAX_LENGTH = 280;

    // pensionInsightIntro
    private static final int PENSION_INSIGHT_INTRO_MAX_LENGTH = 150;

    // pensionInsightStrategy
    private static final int PENSION_INSIGHT_STRATEGY_MIN_LENGTH = 45;
    private static final int PENSION_INSIGHT_STRATEGY_MAX_LENGTH = 120;

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

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public YounglyRecommendationResponse generateRecommendation(RecommendationPrompt prompt) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("openai.api-key가 설정되어 있지 않습니다.");
        }
        if (!StringUtils.hasText(model)) {
            throw new IllegalStateException("openai.model이 설정되어 있지 않습니다.");
        }

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5_000)
                .setConnectionRequestTimeout(5_000)
                .setSocketTimeout(120_000)
                .build();

        HttpPost request = new HttpPost(RESPONSES_API_URL);
        request.setHeader("Authorization", "Bearer " + apiKey);
        request.setHeader("Content-Type", "application/json");

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            request.setEntity(new StringEntity(
                    objectMapper.writeValueAsString(buildRequestBody(prompt)),
                    ContentType.APPLICATION_JSON
            ));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                String responseBody = response.getEntity() == null
                        ? ""
                        : EntityUtils.toString(response.getEntity());

                if (status < 200 || status >= 300) {
                    throw new IllegalStateException(
                            "OpenAI Responses API 호출에 실패했습니다. status="
                                    + status + ", body=" + snippet(responseBody)
                    );
                }

                String outputText = extractOutputText(responseBody);
                YounglyRecommendationResponse recommendation =
                        objectMapper.readValue(outputText, YounglyRecommendationResponse.class);

                validateResponse(recommendation);
                return recommendation;
            }
        } catch (IOException e) {
            throw new IllegalStateException("OpenAI 추천 응답 생성에 실패했습니다.", e);
        }
    }

    private Map<String, Object> buildRequestBody(RecommendationPrompt prompt) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("model", model);
        body.put("max_output_tokens", 4000);
        body.put("reasoning", Map.of("effort", "minimal"));

        body.put("input", List.of(
                message("system", prompt.systemMessage()),
                message("user", prompt.userMessage())
        ));

        Map<String, Object> text = new LinkedHashMap<>();
        text.put("verbosity", "low");
        text.put("format", responseFormat());

        body.put("text", text);

        return body;
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private Map<String, Object> responseFormat() {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "youngly_recommendation_response");
        format.put("strict", true);
        format.put("schema", responseSchema());
        return format;
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of(
                "marketHighlights",
                "marketDetail",
                "pensionInsightIntro",
                "pensionInsightStrategy"
        ));

        Map<String, Object> properties = new LinkedHashMap<>();

        properties.put("marketHighlights", Map.of(
                "type", "array",
                "minItems", MARKET_HIGHLIGHTS_MIN_ITEMS,
                "maxItems", MARKET_HIGHLIGHTS_MAX_ITEMS,
                "items", Map.of("type", "string", "maxLength", MARKET_HIGHLIGHT_MAX_LENGTH)
        ));

        properties.put("marketDetail", Map.of(
                "type", "string",
                "minLength", MARKET_DETAIL_MIN_LENGTH,
                "maxLength", MARKET_DETAIL_MAX_LENGTH
        ));

        properties.put("pensionInsightIntro", Map.of(
                "type", "string",
                "maxLength", PENSION_INSIGHT_INTRO_MAX_LENGTH
        ));

        properties.put("pensionInsightStrategy", Map.of(
                "type", "string",
                "minLength", PENSION_INSIGHT_STRATEGY_MIN_LENGTH,
                "maxLength", PENSION_INSIGHT_STRATEGY_MAX_LENGTH
        ));

        schema.put("properties", properties);

        return schema;
    }

    private String extractOutputText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode outputText = root.get("output_text");
        if (outputText != null && outputText.isTextual() && StringUtils.hasText(outputText.asText())) {
            return outputText.asText();
        }

        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode outputItem : output) {
                JsonNode content = outputItem.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode contentItem : content) {
                    JsonNode text = contentItem.get("text");
                    if (text != null && text.isTextual() && StringUtils.hasText(text.asText())) {
                        return text.asText();
                    }
                }
            }
        }

        throw new IllegalStateException(
                "OpenAI 응답에서 구조화 출력 텍스트를 찾을 수 없습니다. body=" + snippet(responseBody)
        );
    }

    private void validateResponse(YounglyRecommendationResponse response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI 추천 응답이 비어 있습니다.");
        }

        if (response.marketHighlights() == null
                || response.marketHighlights().size() < MARKET_HIGHLIGHTS_MIN_ITEMS
                || response.marketHighlights().size() > MARKET_HIGHLIGHTS_MAX_ITEMS) {
            throw new IllegalStateException("OpenAI 추천 응답 marketHighlights 항목 수가 올바르지 않습니다.");
        }
        for (String item : response.marketHighlights()) {
            validateMaxLength("marketHighlights", item, MARKET_HIGHLIGHT_MAX_LENGTH);
        }

        validateRange("marketDetail", response.marketDetail(),
                MARKET_DETAIL_MIN_LENGTH, MARKET_DETAIL_MAX_LENGTH);

        validateMaxLength("pensionInsightIntro", response.pensionInsightIntro(),
                PENSION_INSIGHT_INTRO_MAX_LENGTH);

        validateRange("pensionInsightStrategy", response.pensionInsightStrategy(),
                PENSION_INSIGHT_STRATEGY_MIN_LENGTH, PENSION_INSIGHT_STRATEGY_MAX_LENGTH);

        String combined = String.join("\n",
                response.marketHighlights() == null ? "" : String.join("\n", response.marketHighlights()),
                nullToEmpty(response.marketDetail()),
                nullToEmpty(response.pensionInsightIntro()),
                nullToEmpty(response.pensionInsightStrategy())
        );

        for (String bannedPhrase : BANNED_PHRASES) {
            if (combined.contains(bannedPhrase)) {
                throw new IllegalStateException(
                        "OpenAI 추천 응답에 금지 표현이 포함되어 있습니다. phrase=" + bannedPhrase
                );
            }
        }
    }

    private void validateMaxLength(String fieldName, String value, int maxLength) {
        if (value == null) {
            throw new IllegalStateException("OpenAI 추천 응답 필수 필드가 null입니다. field=" + fieldName);
        }
        if (value.length() > maxLength) {
            throw new IllegalStateException(
                    "OpenAI 추천 응답 길이 제한을 초과했습니다. field=" + fieldName
                            + ", length=" + value.length() + ", maxLength=" + maxLength
            );
        }
    }

    private void validateRange(String fieldName, String value, int minLength, int maxLength) {
        if (value == null) {
            throw new IllegalStateException("OpenAI 추천 응답 필수 필드가 null입니다. field=" + fieldName);
        }
        if (value.length() < minLength || value.length() > maxLength) {
            throw new IllegalStateException(
                    "OpenAI 추천 응답 길이 범위를 벗어났습니다. field=" + fieldName
                            + ", length=" + value.length()
                            + ", minLength=" + minLength
                            + ", maxLength=" + maxLength
            );
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String snippet(String body) {
        if (!StringUtils.hasText(body)) {
            return "";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(0, 500);
    }
}