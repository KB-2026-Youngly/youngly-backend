package com.kb.youngly.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 서버 기동 확인용 헬스체크 컨트롤러.
 *
 * [INFO] 초기 세팅 검증 전용이다. 기능 개발이 시작되면 각 담당자는
 * 이 클래스를 수정하지 말고 별도 컨트롤러를 추가한다.
 */
@RestController
@RequestMapping("/api")
@Log4j2
public class HomeController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        log.info("[INFO] health check 요청 수신");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "youngly-backend");
        body.put("javaVersion", System.getProperty("java.version"));
        body.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return body;
    }
}
