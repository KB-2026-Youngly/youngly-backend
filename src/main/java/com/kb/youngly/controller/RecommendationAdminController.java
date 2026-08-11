package com.kb.youngly.controller;

import com.kb.youngly.dto.recommendation.RecommendationResponse;
import com.kb.youngly.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자용 AI 연금 코치 리포트 생성 엔드포인트.
 * 현재는 테스트/검증용으로만 사용하며, 추후 사용자용 API(YL-27)에서
 * 경로와 권한을 재정의할 수 있다.
 */
@Log4j2
@RestController
@RequestMapping("/api/admin/recommendations")
@RequiredArgsConstructor
public class RecommendationAdminController {

    private final RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public ResponseEntity<RecommendationResponse> generateRecommendation(@PathVariable String userId) {
        log.info("[INFO] AI 연금 코치 리포트 생성 요청. userId={}", userId);
        RecommendationResponse response = recommendationService.generateRecommendation(userId);
        return ResponseEntity.ok(response);
    }
}