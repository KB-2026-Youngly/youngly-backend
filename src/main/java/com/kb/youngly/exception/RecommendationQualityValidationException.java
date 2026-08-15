package com.kb.youngly.exception;

public class RecommendationQualityValidationException extends IllegalStateException {

    private final String failureType;
    private final String fieldName;
    private final Integer length;
    private final String lastChar;
    private final String bannedTerm;
    private final boolean hardFailure;

    public RecommendationQualityValidationException(String message) {
        this(message, "QUALITY_VALIDATION", null, null, null, null, true);
    }

    public RecommendationQualityValidationException(String message,
                                                    String failureType,
                                                    String fieldName,
                                                    Integer length,
                                                    String lastChar,
                                                    String bannedTerm) {
        this(message, failureType, fieldName, length, lastChar, bannedTerm, true);
    }

    public RecommendationQualityValidationException(String message,
                                                    String failureType,
                                                    String fieldName,
                                                    Integer length,
                                                    String lastChar,
                                                    String bannedTerm,
                                                    boolean hardFailure) {
        super(message);
        this.failureType = failureType;
        this.fieldName = fieldName;
        this.length = length;
        this.lastChar = lastChar;
        this.bannedTerm = bannedTerm;
        this.hardFailure = hardFailure;
    }

    public String getFailureType() {
        return failureType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Integer getLength() {
        return length;
    }

    public String getLastChar() {
        return lastChar;
    }

    public String getBannedTerm() {
        return bannedTerm;
    }

    public boolean isHardFailure() {
        return hardFailure;
    }
}
