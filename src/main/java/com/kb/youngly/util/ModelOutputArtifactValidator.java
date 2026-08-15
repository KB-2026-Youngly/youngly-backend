package com.kb.youngly.util;

import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.exception.RecommendationQualityValidationException;

import java.util.List;
import java.util.regex.Pattern;

public final class ModelOutputArtifactValidator {

    private static final String FAILURE_TYPE = "MODEL_OUTPUT_ARTIFACT";

    private static final List<String> DIRECT_ARTIFACTS = List.of(
            "<analysis",
            "</analysis",
            "<final",
            "</final",
            "```",
            "<|",
            "|>",
            "\"} }",
            "\"] }",
            "} } }",
            "”}",
            "\"}",
            "]}"
    );

    private static final Pattern CLOSING_TAG =
            Pattern.compile("</[A-Za-z][A-Za-z0-9_-]*\\s*>");
    private static final Pattern ROLE_TOKEN =
            Pattern.compile("(?i)(^|[\\s`\"'{}\\[\\]<>/:])(?:assistant|system|user|tool)(?=$|[\\s`\"'{}\\[\\]<>/:])");
    private static final Pattern SAME_TOKEN_REPEATED_TEN_OR_MORE =
            Pattern.compile("(?s)(^|\\s)(\\S+)(?:\\s+\\2){9,}(?=\\s|$)");
    private static final Pattern LONG_NUMERIC_TAIL =
            Pattern.compile(".*[.!?。요다]\\s*[0-9](?:\\s*[0-9]){19,}\\s*$");

    private ModelOutputArtifactValidator() {
    }

    public static void validateRecommendationResponse(RecommendationResponse response) {
        if (response == null) {
            return;
        }

        validateMarketHighlights(response.marketHighlights());
        validate("marketDetail", response.marketDetail());
        validate("pensionInsightIntro", response.pensionInsightIntro());
        validate("pensionInsightStrategy", response.pensionInsightStrategy());
    }

    public static void validateMarketHighlights(List<String> marketHighlights) {
        if (marketHighlights == null) {
            return;
        }

        for (int index = 0; index < marketHighlights.size(); index++) {
            validate("marketHighlights[" + index + "]", marketHighlights.get(index));
        }
    }

    public static void validate(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        String artifact = findArtifact(value);
        if (artifact != null) {
            throw artifactException(fieldName, value, artifact);
        }
    }

    public static boolean containsArtifact(String value) {
        return findArtifact(value) != null;
    }

    private static String findArtifact(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (String artifact : DIRECT_ARTIFACTS) {
            if (value.contains(artifact)) {
                return artifact;
            }
        }

        if (CLOSING_TAG.matcher(value).find()) {
            return "closing-tag";
        }

        if (ROLE_TOKEN.matcher(value).find()) {
            return "role-token";
        }

        if (SAME_TOKEN_REPEATED_TEN_OR_MORE.matcher(value).find()) {
            return "repeated-token";
        }

        if (LONG_NUMERIC_TAIL.matcher(value).matches()) {
            return "numeric-tail";
        }

        return null;
    }

    private static RecommendationQualityValidationException artifactException(
            String fieldName,
            String value,
            String bannedTerm
    ) {
        return new RecommendationQualityValidationException(
                "모델 출력 오염 토큰이 포함되어 있습니다. field="
                        + fieldName + ", artifact=" + bannedTerm,
                FAILURE_TYPE,
                fieldName,
                value == null ? null : value.length(),
                lastChar(value),
                bannedTerm,
                true
        );
    }

    private static String lastChar(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? "" : trimmed.substring(trimmed.length() - 1);
    }
}
