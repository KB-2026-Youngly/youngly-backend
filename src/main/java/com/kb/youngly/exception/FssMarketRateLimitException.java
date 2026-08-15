package com.kb.youngly.exception;

public class FssMarketRateLimitException extends FssMarketApiException {

    public static final String RESULT_CODE = "033";

    public FssMarketRateLimitException(String message) {
        super(message);
    }
}
