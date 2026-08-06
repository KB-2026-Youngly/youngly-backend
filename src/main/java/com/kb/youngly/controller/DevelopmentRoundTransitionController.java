package com.kb.youngly.controller;

import com.kb.youngly.dto.round.RoundTransitionExecutionResponse;
import com.kb.youngly.service.RoundTransitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Postman으로 라운드 자동 전환을 확인하기 위한 개발 전용 API.
 * 운영 배포 전에는 이 Controller와 {@code SecurityConfig}의 {@code /api/dev/**}
 * 허용 설정을 제거해야 한다.
 */
@RestController
@RequestMapping("/api/dev/round-transitions")
@RequiredArgsConstructor
public class DevelopmentRoundTransitionController {

    /** 스케줄러와 동일한 전환 대상 조회 및 그룹별 전환 로직을 수행한다. */
    private final RoundTransitionService roundTransitionService;

    /**
     * 전달받은 날짜를 기준으로 자동 스케줄러와 동일한 라운드 전환을 실행한다.
     *
     * <p>예: {@code POST /api/dev/round-transitions?date=2026-08-05}</p>
     *
     * @param date {@code rounds.end_date}와 비교할 테스트 기준일
     * @return 대상, 성공, 건너뜀, 실패 건수와 그룹별 실패 사유
     */
    @PostMapping
    public ResponseEntity<RoundTransitionExecutionResponse> transition(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // 스케줄러와 같은 조건으로 진행 중이며 지정 날짜에 종료되는 그룹을 조회한다.
        List<String> groupIds = roundTransitionService.findDueGroupIds(date);
        List<String> failures = new ArrayList<>();
        int transitionedCount = 0;
        int skippedCount = 0;

        for (String groupId : groupIds) {
            try {
                // true는 전환 완료, false는 중복 실행 등으로 처리 대상이 사라진 경우다.
                if (roundTransitionService.transitionToNextRound(groupId, date)) {
                    transitionedCount++;
                } else {
                    skippedCount++;
                }
            } catch (RuntimeException exception) {
                // 한 그룹의 실패가 나머지 테스트 대상의 전환을 막지 않도록 개별 기록한다.
                failures.add("groupId=" + groupId + ", message=" + exception.getMessage());
            }
        }

        RoundTransitionExecutionResponse response =
                RoundTransitionExecutionResponse.builder()
                        .transitionDate(date)
                        .targetCount(groupIds.size())
                        .transitionedCount(transitionedCount)
                        .skippedCount(skippedCount)
                        .failedCount(failures.size())
                        .failures(failures)
                        .build();

        return ResponseEntity.ok(response);
    }
}
