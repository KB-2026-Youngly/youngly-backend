package com.kb.youngly.exception;

/**
 * 금감원 금융시장동향 API 호출/응답 처리 실패 시 사용.
 */
public class FssMarketApiException extends RuntimeException {

    public FssMarketApiException(String message) {
        super(message);
    }

    public FssMarketApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
