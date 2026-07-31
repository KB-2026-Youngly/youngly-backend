package com.kb.youngly.vo.survey;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SurveyResultVO {
    private Long surveyResultId;
    private String userId;
    private String answersJson;
    private Integer totalScore;
    private String baseline;
    private LocalDateTime submittedAt;
    private LocalDateTime calculatedAt;
    private LocalDateTime createdAt;
}