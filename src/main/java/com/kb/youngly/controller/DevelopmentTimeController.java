package com.kb.youngly.controller;

import com.kb.youngly.dto.demo.DemoTimeAdvanceRequest;
import com.kb.youngly.dto.demo.DemoTimeApplyRequest;
import com.kb.youngly.dto.demo.DemoTimeResponse;
import com.kb.youngly.dto.round.DailyBatchExecutionResponse;
import com.kb.youngly.scheduler.DailyBatchScheduler;
import com.kb.youngly.service.MarketSnapshotOrchestrationService;
import com.kb.youngly.util.YounglyTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 발표 및 시연 중 서비스 가상 날짜와 예약 작업 실행을 제어하는 개발 전용 API. */
@RestController
@RequestMapping("/api/dev/time")
@RequiredArgsConstructor
public class DevelopmentTimeController {

    private static final int MAX_ADVANCE_DAYS = 365;

    private final DailyBatchScheduler dailyBatchScheduler;
    private final MarketSnapshotOrchestrationService marketSnapshotOrchestrationService;
    private final Object executionLock = new Object();

    @GetMapping
    public ResponseEntity<DemoTimeResponse> getCurrentTime() {
        return ResponseEntity.ok(status(null, YounglyTime.today(), 0, 0,
                Collections.emptyList(), null));
    }

    @PostMapping("/advance")
    public ResponseEntity<DemoTimeResponse> advance(
            @RequestBody DemoTimeAdvanceRequest request) {
        if (request == null || request.getDays() == null) {
            throw new IllegalArgumentException("이동할 일수가 필요합니다.");
        }
        validateAdvanceDays(request.getDays());

        synchronized (executionLock) {
            LocalDate previousDate = YounglyTime.today();
            LocalDate targetDate = previousDate.plusDays(request.getDays());
            return ResponseEntity.ok(executeForward(
                    previousDate,
                    targetDate,
                    Boolean.TRUE.equals(request.getIncludeMarketScheduler())
            ));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<DemoTimeResponse> apply(
            @RequestBody DemoTimeApplyRequest request) {
        if (request == null || request.getDate() == null) {
            throw new IllegalArgumentException("적용할 날짜가 필요합니다.");
        }

        synchronized (executionLock) {
            LocalDate previousDate = YounglyTime.today();
            LocalDate targetDate = request.getDate();
            long difference = targetDate.toEpochDay() - previousDate.toEpochDay();

            if (difference > MAX_ADVANCE_DAYS) {
                throw new IllegalArgumentException("한 번에 이동할 수 있는 기간은 최대 365일입니다.");
            }
            if (difference > 0) {
                return ResponseEntity.ok(executeForward(
                        previousDate,
                        targetDate,
                        Boolean.TRUE.equals(request.getIncludeMarketScheduler())
                ));
            }

            YounglyTime.setDate(targetDate);
            String warning = difference < 0
                    ? "과거 날짜로 이동해도 이미 처리된 DB 데이터는 되돌아가지 않습니다."
                    : null;
            return ResponseEntity.ok(status(previousDate, targetDate, 0, 0,
                    Collections.emptyList(), warning));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<DemoTimeResponse> reset() {
        synchronized (executionLock) {
            LocalDate previousDate = YounglyTime.today();
            YounglyTime.reset();
            return ResponseEntity.ok(status(previousDate, YounglyTime.today(), 0, 0,
                    Collections.emptyList(), "실제 서버 날짜로 돌아왔습니다."));
        }
    }

    private DemoTimeResponse executeForward(
            LocalDate previousDate,
            LocalDate targetDate,
            boolean includeMarketScheduler) {
        List<DailyBatchExecutionResponse> results = new ArrayList<>();
        int marketExecutionCount = 0;

        for (LocalDate date = previousDate.plusDays(1);
             !date.isAfter(targetDate);
             date = date.plusDays(1)) {
            YounglyTime.setDate(date);
            results.add(dailyBatchScheduler.executeDailyBatch(date));

            if (includeMarketScheduler) {
                marketSnapshotOrchestrationService
                        .reingestRecentTwoWeeksAndSummarize("DEMO_TIME_CONTROL");
                marketExecutionCount++;
            }
        }

        return status(previousDate, targetDate, results.size(), marketExecutionCount,
                results, null);
    }

    private DemoTimeResponse status(
            LocalDate previousDate,
            LocalDate targetDate,
            int executedDayCount,
            int marketExecutionCount,
            List<DailyBatchExecutionResponse> dailyBatches,
            String warning) {
        return DemoTimeResponse.builder()
                .overridden(YounglyTime.isOverridden())
                .currentDateTime(YounglyTime.now())
                .previousDate(previousDate)
                .targetDate(targetDate)
                .executedDayCount(executedDayCount)
                .marketSchedulerExecutionCount(marketExecutionCount)
                .dailyBatches(dailyBatches)
                .warning(warning)
                .build();
    }

    private void validateAdvanceDays(int days) {
        if (days < 1 || days > MAX_ADVANCE_DAYS) {
            throw new IllegalArgumentException("이동 일수는 1일 이상 365일 이하여야 합니다.");
        }
    }
}
