package com.kb.youngly.vo.survey;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class SurveyChoiceVO {
    private long choiceId;
    private long questionId;
    private String choiceText;
    private int score;
    private int displayOrder;
    private Timestamp createdAt;
}
