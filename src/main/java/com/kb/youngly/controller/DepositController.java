package com.kb.youngly.controller;

import com.kb.youngly.dto.deposit.DepositRequest;
import com.kb.youngly.dto.deposit.DepositResponse;
import com.kb.youngly.dto.deposit.MemberDepositStatusResponse;
import com.kb.youngly.service.DepositService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 그룹 예치금 납부 및 예치 현황 조회 REST API.
 *
 * <p>현재 인증 기능이 아직 연결되지 않아 {@code X-User-Id} 헤더를 임시로
 * 사용한다. 인증 도입 후에는 이 값을 인증 객체의 사용자 ID로 교체한다.</p>
 */
@RestController
@RequestMapping("/api/groups/{groupId}")
@RequiredArgsConstructor
public class DepositController {

    /** 로그인 연동 전 요청 사용자를 식별하는 임시 헤더. */
    private static final String USER_ID_HEADER = "X-User-Id";

    private final DepositService depositService;

    /** 개인 계좌에서 그룹 모임통장으로 예치금을 납부한다. */
    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> deposit(
            @RequestHeader(USER_ID_HEADER) String userId,
            @PathVariable String groupId,
            @RequestBody DepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(depositService.deposit(userId, groupId, request));
    }

    /** 요청 사용자의 현재 예치금 및 추가 납부 필요 금액을 조회한다. */
    @GetMapping("/deposit/me")
    public DepositResponse getMyDeposit(
            @RequestHeader(USER_ID_HEADER) String userId,
            @PathVariable String groupId) {
        return depositService.getMyDeposit(userId, groupId);
    }

    /** 그룹에 참여한 사용자가 전체 참여자의 예치 현황을 조회한다. */
    @GetMapping("/deposit")
    public List<MemberDepositStatusResponse> getMemberDepositStatuses(
            @RequestHeader(USER_ID_HEADER) String userId,
            @PathVariable String groupId) {
        return depositService.getMemberDepositStatuses(userId, groupId);
    }
}
