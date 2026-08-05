package com.kb.youngly.controller;

import com.kb.youngly.dto.round.WeeklySettlementExecutionResponse;
import com.kb.youngly.dto.round.WeeklySettlementTarget;
import com.kb.youngly.service.WeeklySettlementService;
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
 * Postman으로 주간 결산을 확인하기 위한 개발 전용 API.
 * 운영 배포 전에는 이 Controller와 SecurityConfig의 허용 경로를 제거해야 한다.
 */
@RestController
@RequestMapping("/api/dev/weekly-settlements")
@RequiredArgsConstructor
public class DevelopmentWeeklySettlementController {

    private final WeeklySettlementService weeklySettlementService;

    /**
     * 전달받은 날짜를 기준으로 자동 스케줄러와 동일한 주간 결산을 실행한다.
     * 예: POST /api/dev/weekly-settlements?date=2026-08-08
     */
    @PostMapping
    public ResponseEntity<WeeklySettlementExecutionResponse> settle(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<WeeklySettlementTarget> targets =
                weeklySettlementService.findDueSettlements(date);
        List<String> failures = new ArrayList<>();
        int successfulParticipantCount = 0;

        // 한 라운드의 실패가 나머지 테스트 대상의 결산을 막지 않도록 개별 처리한다.
        for (WeeklySettlementTarget target : targets) {
            try {
                successfulParticipantCount += weeklySettlementService.settle(target);
            } catch (RuntimeException exception) {
                failures.add("roundId=" + target.getRoundId()
                        + ", weekNo=" + target.getWeekNo()
                        + ", message=" + exception.getMessage());
            }
        }

        WeeklySettlementExecutionResponse response =
                WeeklySettlementExecutionResponse.builder()
                        .settlementDate(date)
                        .targetCount(targets.size())
                        .failedTargetCount(failures.size())
                        .successfulParticipantCount(successfulParticipantCount)
                        .failures(failures)
                        .build();

        return ResponseEntity.ok(response);
    }
}
