package com.kb.youngly.exception;

/**
 * 투자성향 설문이 완료되지 않은 사용자가 개인연금 인사이트를 요청할 때 발생한다.
 * HTTP 409 Conflict로 매핑된다.
 */
public class SurveyNotCompletedException extends RuntimeException {

    public SurveyNotCompletedException(String message) {
        super(message);
    }
}
