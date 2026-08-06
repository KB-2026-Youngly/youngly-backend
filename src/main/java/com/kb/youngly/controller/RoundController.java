package com.kb.youngly.controller;

import com.kb.youngly.dto.round.CreateRoundRequest;
import com.kb.youngly.dto.round.CreateRoundResponse;
import com.kb.youngly.service.RoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 그룹별 라운드 생성 REST API. */
@RestController
@RequestMapping("/api/groups/{groupId}")
@RequiredArgsConstructor
public class RoundController {

    /** 인증 연동 전 라운드 생성 API 호출에 사용할 임시 사용자 ID. */
    private static final String DEVELOPMENT_USER_ID = "ab307e87-a99c-4108-9d6f-656768636a92";

    private final RoundService roundService;

    /**
     * 로그인 사용자가 그룹의 다음 라운드를 생성한다.
     * 첫 라운드는 시작일을 요청 본문으로 받고, 이후 라운드는 요청 본문을 생략할 수 있다.
     * 생성이 완료되면 HTTP 201과 생성 결과를 반환한다.
     */
    @PostMapping("/rounds")
    public ResponseEntity<CreateRoundResponse> createRound(
            @PathVariable String groupId,
            @RequestBody(required = false) CreateRoundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roundService.createRound(DEVELOPMENT_USER_ID, groupId, request));
    }
}
