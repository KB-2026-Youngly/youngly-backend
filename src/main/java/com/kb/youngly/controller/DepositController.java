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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 그룹 예치금 납부 및 예치 현황 조회 REST API.
 *
 * <p>개발 중에는 인증 연동 전 예치금 흐름 확인을 위해 {@code user01}을 사용한다.
 * 운영 배포 전에는 JWT 인증 객체의 사용자 ID를 사용하도록 반드시 되돌려야 한다.</p>
 */
@RestController
@RequestMapping("/api/groups/{groupId}")
@RequiredArgsConstructor
public class DepositController {

    /** 인증 연동 전 API 호출을 위한 임시 사용자 ID. */
    private static final String DEVELOPMENT_USER_ID = "user01";

    private final DepositService depositService;

    /** 개인 계좌에서 그룹 모임통장으로 예치금을 납부한다. */
    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> deposit(
            @PathVariable String groupId,
            @RequestBody DepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(depositService.deposit(DEVELOPMENT_USER_ID, groupId, request));
    }

    /** 로그인 사용자의 예치금과 추가 납부 필요 금액을 조회한다. */
    @GetMapping("/deposit/me")
    public DepositResponse getMyDeposit(@PathVariable String groupId) {
        return depositService.getMyDeposit(DEVELOPMENT_USER_ID, groupId);
    }

    /** 그룹 참여자만 전체 참여자의 예치 현황을 조회할 수 있다. */
    @GetMapping("/deposit")
    public List<MemberDepositStatusResponse> getMemberDepositStatuses(@PathVariable String groupId) {
        return depositService.getMemberDepositStatuses(DEVELOPMENT_USER_ID, groupId);
    }
}
