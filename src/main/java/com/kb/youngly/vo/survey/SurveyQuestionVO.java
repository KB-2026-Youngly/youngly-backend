package com.kb.youngly.vo.survey;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SurveyQuestionVO {
    private Long questionId;
    private Integer questionNo;
    private String questionText;
    private Boolean isMultiple;
    private LocalDateTime createdAt;
}
