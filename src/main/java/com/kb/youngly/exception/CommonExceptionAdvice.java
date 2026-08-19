package com.kb.youngly.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import com.kb.youngly.util.YounglyTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 전역 예외 처리기.
 *
 * [INFO] 프론트엔드가 Vue SPA 이므로 교안의 JSP(error_page.jsp) 대신
 * 일관된 JSON 형식으로 오류를 응답한다. 응답 스키마는 팀 API 명세서와 동일하게 유지한다.
 */
@RestControllerAdvice
@Log4j2
public class CommonExceptionAdvice {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoHandlerFoundException e,
                                                              HttpServletRequest request) {
        log.warn("[WARN] 존재하지 않는 경로 요청. uri={}", request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, "요청하신 경로를 찾을 수 없습니다.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e,
                                                                HttpServletRequest request) {
        log.warn("[WARN] 잘못된 요청. message={}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e,
                                                                  HttpServletRequest request) {
        log.warn("[WARN] 접근 권한 없음. message={}", e.getMessage());
        return build(HttpStatus.FORBIDDEN, e.getMessage(), request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFoundResource(NoSuchElementException e,
                                                                      HttpServletRequest request) {
        log.warn("[WARN] 리소스 없음. message={}", e.getMessage());
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(SurveyNotCompletedException.class)
    public ResponseEntity<Map<String, Object>> handleSurveyNotCompleted(SurveyNotCompletedException e,
                                                                        HttpServletRequest request) {
        log.warn("[WARN] 설문 미완료. message={}", e.getMessage());
        return build(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception e, HttpServletRequest request) {
        log.error("[ERROR] 처리되지 않은 예외 발생. uri={}", request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", request);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status,
                                                      String message,
                                                      HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", YounglyTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
