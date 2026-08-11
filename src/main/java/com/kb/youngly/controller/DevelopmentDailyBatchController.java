package com.kb.youngly.controller;

import com.kb.youngly.dto.round.DailyBatchExecutionResponse;
import com.kb.youngly.scheduler.DailyBatchScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 자정 통합 배치를 지정 날짜로 실행하는 개발 전용 Controller. */
@RestController
@RequestMapping("/api/dev/daily-batches")
@RequiredArgsConstructor
public class DevelopmentDailyBatchController {

    private final DailyBatchScheduler dailyBatchScheduler;

    /**
     * 자동 스케줄과 동일한 순서로 일일 배치를 실행한다.
     *
     * <p>예: {@code POST /api/dev/daily-batches?date=2026-08-11}</p>
     *
     * <p>전달한 날짜를 자정 배치 실행일로 사용하므로 라운드 전환 대상 종료일은
     * {@code date - 1일}, 최종 정산 대상 종료일은 {@code date - 2일}이다.</p>
     */
    @PostMapping
    public ResponseEntity<DailyBatchExecutionResponse> execute(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dailyBatchScheduler.executeDailyBatch(date));
    }
}
