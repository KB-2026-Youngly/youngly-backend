package com.kb.youngly.controller;

import com.kb.youngly.dto.point.PointBalanceResponse;
import com.kb.youngly.dto.point.PointEarnRequest;
import com.kb.youngly.dto.point.PointEarnResponse;
import com.kb.youngly.dto.point.PointHistoryResponse;
import com.kb.youngly.service.PointService;
import com.kb.youngly.vo.PointHistoryVO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 인증 정보의 사용자 ID를 기준으로 포인트 지급, 잔액 조회, 내역 조회 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    /**
     * 요청한 포인트를 지급하고 새로 생성된 적립 내역을 반환합니다.
     */
    @PostMapping("/earn")
    public ResponseEntity<PointEarnResponse> earnPoint(
            Authentication authentication,
            @RequestBody PointEarnRequest request) {
        String userId = getAuthenticatedUserId(authentication);
        PointHistoryVO history =
                pointService.earnPoint(userId, request.getAmount(), request.getContent());

        return ResponseEntity.ok(PointEarnResponse.from(history));
    }

    /**
     * 인증된 사용자의 현재 보유 포인트를 조회합니다.
     */
    @GetMapping("/balance")
    public ResponseEntity<PointBalanceResponse> getBalance(Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(new PointBalanceResponse(pointService.getPoint(userId)));
    }

    /**
     * 포인트 내역을 최신순으로 조회합니다.
     * limit/offset 기본값은 20/0이며, limit 1~100과 offset 0 이상 여부는 서비스에서 검증합니다.
     */
    @GetMapping("/history")
    public ResponseEntity<List<PointHistoryResponse>> getHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset) {
        String userId = getAuthenticatedUserId(authentication);
        // 내부 조회 결과는 사용자 ID를 제외한 API 응답 DTO로 변환합니다.
        List<PointHistoryResponse> response = pointService
                .getPointHistories(userId, limit, offset)
                .stream()
                .map(PointHistoryResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 요청값을 직접 신뢰하지 않고 JWT 필터가 SecurityContext에 저장한 principal(userId)을 사용합니다.
     */
    private String getAuthenticatedUserId(Authentication authentication) {
        // 인증 객체나 사용자 ID가 없으면 사용자별 포인트 처리를 중단합니다.
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("인증된 사용자 정보가 없습니다.");
        }
        return authentication.getName();
    }
}
