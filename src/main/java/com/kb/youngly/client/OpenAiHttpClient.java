package com.kb.youngly.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.youngly.dto.market.MarketSummaryResponse;
import com.kb.youngly.dto.recommendation.RecommendationPrompt;
import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.util.ModelOutputArtifactValidator;
import com.kb.youngly.exception.RecommendationQualityValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger log = LogManager.getLogger(OpenAiHttpClient.class);

    private static final String RESPONSES_API_URL = "https://api.openai.com/v1/responses";

    private static final int MARKET_HIGHLIGHTS_MIN_ITEMS = 4;
    private static final int MARKET_HIGHLIGHTS_MAX_ITEMS = 4;
    private static final int MARKET_HIGHLIGHT_MAX_LENGTH = 30;

    private static final int MARKET_DETAIL_MIN_LENGTH = 150;
    private static final int MARKET_DETAIL_MAX_LENGTH = 280;

    private static final int MARKET_SUMMARY_HEADLINE_MIN_LENGTH = 10;
    private static final int MARKET_SUMMARY_HEADLINE_MAX_LENGTH = 200;
    private static final int MARKET_SUMMARY_DETAIL_MIN_LENGTH = 80;
    private static final int MARKET_SUMMARY_DETAIL_MAX_LENGTH = 700;

    /*
     * Validator 제한은 화면 품질 계약이다.
     * JSON Schema 상한은 이보다 여유 있게 둬서, 모델이 경계에서 문장을 잘리지 않게 한다.
     */
    private static final int PENSION_INSIGHT_INTRO_MIN_LENGTH = 60;
    private static final int PENSION_INSIGHT_INTRO_MAX_LENGTH = 220;
    private static final int PENSION_INSIGHT_INTRO_SCHEMA_MAX_LENGTH = 400;

    private static final int PENSION_INSIGHT_STRATEGY_MIN_LENGTH = 60;
    private static final int PENSION_INSIGHT_STRATEGY_MAX_LENGTH = 220;
    private static final int PENSION_INSIGHT_STRATEGY_SCHEMA_MAX_LENGTH = 200;

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
            "작성합니다"
    );

    private static final List<String> PERSONAL_INSIGHT_BANNED_PHRASES = List.of(
            "고객님은",
            "고객님의",
            "응답하셨고",
            "타깃 분야",
            "여유 자금",
            "자산배분 관점",
            "투자성향에 맞춰",
            "중기 목표를 지원",
            "지원하세요",
            "인사이트를",
            "점이 반영됩니다",
            "해당 분야",
            "금융 이해도 기초",
            "실제로 관심 있는 분야를 한 가지로 좁히되"
    );

    private static final List<String> GENERIC_STRATEGY_PHRASES = List.of(
            "분산 투자와 장기 관점으로 자금을 운영하고",
            "장기 관점으로 자금을 운영하고",
            "꾸준한 적립으로 목표를 지원",
            "분산 투자와 꾸준한 적립",
            "장기 관점과 분산 투자"
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

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public RecommendationResponse generateRecommendation(RecommendationPrompt prompt) {
        validateConfiguration();

        RequestConfig requestConfig = requestConfig();
        HttpPost request = createRequest();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            request.setEntity(new StringEntity(
                    objectMapper.writeValueAsString(buildRecommendationRequestBody(prompt)),
                    ContentType.APPLICATION_JSON
            ));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = validateAndReadResponse(response, "추천");
                String outputText = extractOutputText(responseBody);
                return parseRecommendationOutputText(outputText);
            }
        } catch (IOException e) {
            throw new IllegalStateException("OpenAI 추천 응답 생성에 실패했습니다.", e);
        }
    }

    @Override
    public MarketSummaryResponse generateMarketSummary(RecommendationPrompt prompt) {
        validateConfiguration();

        RequestConfig requestConfig = requestConfig();
        HttpPost request = createRequest();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            request.setEntity(new StringEntity(
                    objectMapper.writeValueAsString(buildMarketSummaryRequestBody(prompt)),
                    ContentType.APPLICATION_JSON
            ));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = validateAndReadResponse(response, "시장요약");
                String outputText = extractOutputText(responseBody);

                MarketSummaryResponse marketSummary =
                        objectMapper.readValue(outputText, MarketSummaryResponse.class);

                validateMarketSummaryResponse(marketSummary);
                return marketSummary;
            }
        } catch (IOException e) {
            throw new IllegalStateException("OpenAI 시장요약 응답 생성에 실패했습니다.", e);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("openai.api-key가 설정되어 있지 않습니다.");
        }
        if (!StringUtils.hasText(model)) {
            throw new IllegalStateException("openai.model이 설정되어 있지 않습니다.");
        }
    }

    private RequestConfig requestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(5_000)
                .setConnectionRequestTimeout(5_000)
                .setSocketTimeout(120_000)
                .build();
    }

    private HttpPost createRequest() {
        HttpPost request = new HttpPost(RESPONSES_API_URL);
        request.setHeader("Authorization", "Bearer " + apiKey);
        request.setHeader("Content-Type", "application/json");
        return request;
    }

    private String validateAndReadResponse(CloseableHttpResponse response, String requestName)
            throws IOException {

        int status = response.getStatusLine().getStatusCode();
        String responseBody = response.getEntity() == null
                ? ""
                : EntityUtils.toString(response.getEntity());

        if (status < 200 || status >= 300) {
            throw new IllegalStateException(
                    "OpenAI " + requestName + " API 호출에 실패했습니다. status="
                            + status + ", body=" + snippet(responseBody)
            );
        }

        return responseBody;
    }

    private Map<String, Object> buildRecommendationRequestBody(RecommendationPrompt prompt) {
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

    private Map<String, Object> buildMarketSummaryRequestBody(RecommendationPrompt prompt) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("model", model);
        body.put("max_output_tokens", 1600);
        body.put("reasoning", Map.of("effort", "minimal"));
        body.put("input", List.of(
                message("system", prompt.systemMessage()),
                message("user", prompt.userMessage())
        ));

        Map<String, Object> text = new LinkedHashMap<>();
        text.put("verbosity", "low");
        text.put("format", marketSummaryResponseFormat());
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

    private Map<String, Object> marketSummaryResponseFormat() {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "youngly_market_summary_response");
        format.put("strict", true);
        format.put("schema", marketSummaryResponseSchema());
        return format;
    }

    private Map<String, Object> marketSummaryResponseSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("headline", "detail"));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("headline", Map.of(
                "type", "string",
                "minLength", MARKET_SUMMARY_HEADLINE_MIN_LENGTH,
                "maxLength", MARKET_SUMMARY_HEADLINE_MAX_LENGTH
        ));
        properties.put("detail", Map.of(
                "type", "string",
                "minLength", MARKET_SUMMARY_DETAIL_MIN_LENGTH,
                "maxLength", MARKET_SUMMARY_DETAIL_MAX_LENGTH
        ));

        schema.put("properties", properties);
        return schema;
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
                "items", Map.of(
                        "type", "string",
                        "maxLength", MARKET_HIGHLIGHT_MAX_LENGTH
                )
        ));

        properties.put("marketDetail", Map.of(
                "type", "string",
                "minLength", MARKET_DETAIL_MIN_LENGTH,
                "maxLength", MARKET_DETAIL_MAX_LENGTH
        ));

        properties.put("pensionInsightIntro", Map.of(
                "type", "string",
                "minLength", PENSION_INSIGHT_INTRO_MIN_LENGTH,
                "maxLength", PENSION_INSIGHT_INTRO_SCHEMA_MAX_LENGTH
        ));

        properties.put("pensionInsightStrategy", Map.of(
                "type", "string",
                "minLength", PENSION_INSIGHT_STRATEGY_MIN_LENGTH,
                "maxLength", PENSION_INSIGHT_STRATEGY_SCHEMA_MAX_LENGTH
        ));

        schema.put("properties", properties);
        return schema;
    }

    private String extractOutputText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        JsonNode outputText = root.get("output_text");
        if (outputText != null
                && outputText.isTextual()
                && StringUtils.hasText(outputText.asText())) {
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
                    if (text != null
                            && text.isTextual()
                            && StringUtils.hasText(text.asText())) {
                        return text.asText();
                    }
                }
            }
        }

        throw new IllegalStateException(
                "OpenAI 응답에서 구조화 출력 텍스트를 찾을 수 없습니다. body="
                        + snippet(responseBody)
        );
    }

    private void validateResponse(RecommendationResponse response) {
        if (response == null) {
            throw qualityException(
                    "OpenAI 추천 응답이 비어 있습니다.",
                    "EMPTY_RESPONSE",
                    null,
                    null,
                    null,
                    null
            );
        }

        validateMarketHighlights(response.marketHighlights());
        ModelOutputArtifactValidator.validateRecommendationResponse(response);

        validateRange(
                "marketDetail",
                response.marketDetail(),
                MARKET_DETAIL_MIN_LENGTH,
                MARKET_DETAIL_MAX_LENGTH
        );

        validateRequiredText("pensionInsightIntro", response.pensionInsightIntro());
        validateRequiredText("pensionInsightStrategy", response.pensionInsightStrategy());
        validateNoDuplicateSentence(response.pensionInsightIntro(), "pensionInsightIntro");
        validateNoDuplicateSentence(response.pensionInsightStrategy(), "pensionInsightStrategy");
        validateNoUnsupportedOrForcefulPhrases(response.pensionInsightStrategy());

        String combined = String.join("\n",
                response.marketHighlights() == null
                        ? ""
                        : String.join("\n", response.marketHighlights()),
                nullToEmpty(response.marketDetail()),
                nullToEmpty(response.pensionInsightIntro()),
                nullToEmpty(response.pensionInsightStrategy())
        );

        validateBannedPhrases(combined);
        validateNoInternalTerms(combined);
        validatePersonalInsightPhrases(
                response.pensionInsightIntro(),
                response.pensionInsightStrategy()
        );
        validateNoPromptLeak(
                response.pensionInsightIntro(),
                response.pensionInsightStrategy()
        );
        validateNoBannedFinancialAdvice(
                response.pensionInsightIntro(),
                response.pensionInsightStrategy()
        );
    }

    static String extractFirstCompleteJsonObject(String text) {
        if (!StringUtils.hasText(text)) {
            throw new RecommendationQualityValidationException(
                    "OpenAI 추천 응답에 완결된 JSON 객체가 없습니다.",
                    "MODEL_OUTPUT_JSON_EXTRACT_FAILED",
                    "outputText",
                    text == null ? null : text.length(),
                    null,
                    null,
                    true
            );
        }

        int start = -1;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);

            if (start < 0) {
                if (current == '{') {
                    start = index;
                    depth = 1;
                }
                continue;
            }

            if (escaped) {
                escaped = false;
                continue;
            }

            if (current == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (current == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (current == '{') {
                depth++;
                continue;
            }

            if (current == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, index + 1);
                }
            }
        }

        throw new RecommendationQualityValidationException(
                "OpenAI 추천 응답에 완결된 JSON 객체가 없습니다.",
                "MODEL_OUTPUT_JSON_EXTRACT_FAILED",
                "outputText",
                text.length(),
                lastCharStatic(text),
                null,
                true
        );
    }

    RecommendationResponse parseRecommendationOutputText(String outputText) {
        String jsonObject = extractFirstCompleteJsonObject(outputText);
        logTrailingOutputIfPresent(outputText, jsonObject);

        RecommendationResponse recommendation;
        try {
            recommendation = objectMapper.readValue(jsonObject, RecommendationResponse.class);
        } catch (JsonProcessingException parseException) {
            throw qualityException(
                    "OpenAI 추천 JSON 객체 파싱에 실패했습니다.",
                    "MODEL_OUTPUT_JSON_PARSE_FAILED",
                    "outputText",
                    jsonObject.length(),
                    lastChar(jsonObject),
                    null
            );
        }

        validateResponse(recommendation);
        return recommendation;
    }

    private void logTrailingOutputIfPresent(String outputText, String jsonObject) {
        int trailingLength = outputText.length() - jsonObject.length();
        if (trailingLength <= 0) {
            return;
        }

        String trailing = outputText.substring(jsonObject.length());
        log.warn(
                "[OPENAI_RECOMMENDATION_TRAILING_OUTPUT_DISCARDED] trailingLength={}, hasArtifact={}",
                trailingLength,
                ModelOutputArtifactValidator.containsArtifact(trailing)
        );
    }

    private void validateMarketHighlights(List<String> marketHighlights) {
        if (marketHighlights == null
                || marketHighlights.size() < MARKET_HIGHLIGHTS_MIN_ITEMS
                || marketHighlights.size() > MARKET_HIGHLIGHTS_MAX_ITEMS) {
            throw qualityException(
                    "OpenAI 추천 응답 marketHighlights 항목 수가 올바르지 않습니다.",
                    "MARKET_HIGHLIGHTS_COUNT",
                    "marketHighlights",
                    marketHighlights == null ? null : marketHighlights.size(),
                    null,
                    null
            );
        }

        for (String item : marketHighlights) {
            validateMaxLength("marketHighlights", item, MARKET_HIGHLIGHT_MAX_LENGTH);
        }
    }

    private void validateMarketSummaryResponse(MarketSummaryResponse response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI 시장요약 응답이 비어 있습니다.");
        }

        validateRange(
                "headline",
                response.headline(),
                MARKET_SUMMARY_HEADLINE_MIN_LENGTH,
                MARKET_SUMMARY_HEADLINE_MAX_LENGTH
        );
        validateRange(
                "detail",
                response.detail(),
                MARKET_SUMMARY_DETAIL_MIN_LENGTH,
                MARKET_SUMMARY_DETAIL_MAX_LENGTH
        );

        String combined = String.join(
                "\n",
                nullToEmpty(response.headline()),
                nullToEmpty(response.detail())
        );

        validateBannedPhrases(combined);
    }

    private void validateBannedPhrases(String text) {
        for (String bannedPhrase : BANNED_PHRASES) {
            if (text.contains(bannedPhrase)) {
                throw qualityException(
                        "OpenAI 응답에 금지 표현이 포함되어 있습니다. phrase=" + bannedPhrase,
                        "BANNED_PHRASE",
                        "combinedText",
                        text.length(),
                        null,
                        bannedPhrase
                );
            }
        }
    }

    private void validatePersonalInsightPhrases(String intro, String strategy) {
        String personalInsight = nullToEmpty(intro) + "\n" + nullToEmpty(strategy);

        for (String phrase : PERSONAL_INSIGHT_BANNED_PHRASES) {
            if (personalInsight.contains(phrase)) {
                throw qualityException(
                        "개인화 인사이트에 부자연스러운 표현이 포함되어 있습니다. phrase=" + phrase,
                        "BANNED_PERSONAL_INSIGHT_PHRASE",
                        "personalInsight",
                        personalInsight.length(),
                        null,
                        phrase
                );
            }
        }

        for (String phrase : GENERIC_STRATEGY_PHRASES) {
            if (nullToEmpty(strategy).contains(phrase)) {
                throw qualityException(
                        "개인화 전략에 일반론 표현이 포함되어 있습니다. phrase=" + phrase,
                        "GENERIC_STRATEGY_PHRASE",
                        "pensionInsightStrategy",
                        strategy == null ? null : strategy.length(),
                        lastChar(strategy),
                        phrase
                );
            }
        }
    }

    private void validateRequiredText(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw qualityException(
                    "OpenAI 추천 응답 필수 개인 인사이트 필드가 비어 있습니다. field=" + fieldName,
                    "EMPTY_INTRO_OR_STRATEGY",
                    fieldName,
                    value == null ? null : value.length(),
                    lastChar(value),
                    null
            );
        }
    }

    private void validateNoDuplicateSentence(String value, String fieldName) {
        String text = nullToEmpty(value).trim();
        if (text.contains("보세요. 보세요.")
                || text.contains("하세요. 하세요.")) {
            throw qualityException(
                    "OpenAI 추천 응답에 동일 종결 표현이 반복되었습니다.",
                    "DUPLICATE_SENTENCE",
                    fieldName,
                    text.length(),
                    lastChar(text),
                    null
            );
        }
    }

    private void validateNoPromptLeak(String intro, String strategy) {
        String personalInsight = nullToEmpty(intro) + "\n" + nullToEmpty(strategy);

        for (String phrase : PROMPT_LEAK_BANNED_PHRASES) {
            if (personalInsight.contains(phrase)) {
                throw qualityException(
                        "OpenAI 추천 응답에 프롬프트 지시 문구가 포함되어 있습니다. phrase=" + phrase,
                        "PROMPT_LEAK",
                        "personalInsight",
                        personalInsight.length(),
                        null,
                        phrase
                );
            }
        }
    }

    private void validateNoRepeatedText(String value, String fieldName) {
        String text = nullToEmpty(value).trim();
        String[] words = text.split("\\s+");
        for (int index = 1; index < words.length; index++) {
            if (words[index].equals(words[index - 1])) {
                throw qualityException(
                        "OpenAI 추천 응답에 동일 어절이 연속 반복되었습니다.",
                        "REPEATED_WORD",
                        fieldName,
                        text.length(),
                        lastChar(text),
                        words[index]
                );
            }
        }

        if (text.contains("보세요. 보세요.")
                || text.contains("하세요. 하세요.")) {
            throw qualityException(
                    "OpenAI 추천 응답에 동일 종결 표현이 반복되었습니다.",
                    "REPEATED_ENDING",
                    fieldName,
                    text.length(),
                    lastChar(text),
                    null
            );
        }
    }

    private void validateNoUnsupportedOrForcefulPhrases(String strategy) {
        String text = nullToEmpty(strategy);
        for (String phrase : UNSUPPORTED_OR_FORCEFUL_PHRASES) {
            if (text.contains(phrase)) {
                throw qualityException(
                        "OpenAI 추천 전략에 미지원 기능 또는 과도한 권유 표현이 포함되어 있습니다.",
                        "UNSUPPORTED_OR_FORCEFUL_PHRASE",
                        "pensionInsightStrategy",
                        text.length(),
                        lastChar(text),
                        phrase
                );
            }
        }
    }

    private void validateNoBannedFinancialAdvice(String intro, String strategy) {
        String personalInsight = nullToEmpty(intro) + "\n" + nullToEmpty(strategy);
        for (String phrase : BANNED_FINANCIAL_ADVICE_PHRASES) {
            if (personalInsight.contains(phrase)) {
                throw qualityException(
                        "OpenAI 추천 응답에 금융 안전 금지 표현이 포함되어 있습니다.",
                        "BANNED_FINANCIAL_ADVICE",
                        "personalInsight",
                        personalInsight.length(),
                        null,
                        phrase
                );
            }
        }
    }

    private void validateNoInternalTerms(String combined) {
        for (String term : INTERNAL_BANNED_TERMS) {
            if (combined.contains(term)) {
                throw qualityException(
                        "OpenAI 추천 응답에 내부 용어가 포함되어 있습니다. term=" + term,
                        "BANNED_INTERNAL_TERM",
                        "combinedText",
                        combined.length(),
                        null,
                        term
                );
            }
        }
    }

    private void validateMaxLength(String fieldName, String value, int maxLength) {
        if (value == null) {
            throw qualityException(
                    "OpenAI 추천 응답 필수 필드가 null입니다. field=" + fieldName,
                    "NULL_FIELD",
                    fieldName,
                    null,
                    null,
                    null
            );
        }

        if (value.length() > maxLength) {
            throw qualityException(
                    "OpenAI 추천 응답 길이 제한을 초과했습니다. field=" + fieldName
                            + ", length=" + value.length()
                            + ", maxLength=" + maxLength,
                    "LENGTH_RANGE",
                    fieldName,
                    value.length(),
                    lastChar(value),
                    null
            );
        }
    }

    private void validateRange(String fieldName, String value, int minLength, int maxLength) {
        if (value == null) {
            throw qualityException(
                    "OpenAI 추천 응답 필수 필드가 null입니다. field=" + fieldName,
                    "NULL_FIELD",
                    fieldName,
                    null,
                    null,
                    null
            );
        }

        if (value.length() < minLength || value.length() > maxLength) {
            throw qualityException(
                    "OpenAI 추천 응답 길이 범위를 벗어났습니다. field=" + fieldName
                            + ", length=" + value.length()
                            + ", minLength=" + minLength
                            + ", maxLength=" + maxLength,
                    "LENGTH_RANGE",
                    fieldName,
                    value.length(),
                    lastChar(value),
                    null
            );
        }
    }

    private void validateEndsWithPeriod(String fieldName, String value) {
        if (!value.trim().endsWith(".")) {
            throw qualityException(
                    "OpenAI 추천 응답이 완결된 문장으로 끝나지 않습니다. field=" + fieldName,
                    "ENDING_PERIOD",
                    fieldName,
                    value.length(),
                    lastChar(value),
                    null
            );
        }
    }

    private void validateOneOrTwoSentences(String value) {
        String trimmed = value == null ? "" : value.trim();
        int sentenceCount = countSentenceEndingPeriods(trimmed);

        if (sentenceCount < 1 || sentenceCount > 2) {
            throw qualityException(
                    "개인화 인사이트는 1~2문장이어야 합니다. count=" + sentenceCount,
                    "INTRO_SENTENCE_COUNT",
                    "pensionInsightIntro",
                    value == null ? null : value.length(),
                    lastChar(value),
                    null
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

    private void validateStrategyEnding(String value) {
        String trimmed = value.trim();
        boolean valid = STRATEGY_ALLOWED_ENDINGS.stream().anyMatch(trimmed::endsWith);

        if (!valid) {
            throw qualityException(
                    "OpenAI 추천 응답 전략 문장 종결어가 올바르지 않습니다.",
                    "STRATEGY_ENDING",
                    "pensionInsightStrategy",
                    value.length(),
                    lastChar(value),
                    null
            );
        }
    }

    private RecommendationQualityValidationException qualityException(
            String message,
            String type,
            String field,
            Integer length,
            String lastChar,
            String bannedTerm
    ) {
        return new RecommendationQualityValidationException(
                message,
                type,
                field,
                length,
                lastChar,
                bannedTerm
        );
    }

    private String lastChar(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? "" : trimmed.substring(trimmed.length() - 1);
    }

    private static String lastCharStatic(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? "" : trimmed.substring(trimmed.length() - 1);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String snippet(String body) {
        if (!StringUtils.hasText(body)) {
            return "";
        }

        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= 500
                ? compact
                : compact.substring(0, 500);
    }
}
