package com.kb.youngly.dto.demo;

import com.kb.youngly.dto.round.DailyBatchExecutionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoTimeResponse {
    private Boolean overridden;
    private LocalDateTime currentDateTime;
    private LocalDate previousDate;
    private LocalDate targetDate;
    private Integer executedDayCount;
    private Integer marketSchedulerExecutionCount;
    private List<DailyBatchExecutionResponse> dailyBatches;
    private String warning;
}
