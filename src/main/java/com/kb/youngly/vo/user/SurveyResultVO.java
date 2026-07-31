package com.kb.youngly.vo.user;

import com.kb.youngly.enums.Baseline;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyResultVO {
    private Long surveyResultId;
    private String userId;
    private String answersJson;
    private Integer totalScore;
    private Baseline baseline;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
