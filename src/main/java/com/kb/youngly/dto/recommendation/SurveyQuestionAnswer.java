package com.kb.youngly.dto.recommendation;

public record SurveyQuestionAnswer(
        Integer questionNo,
        String questionText,
        String choiceText
) {
}
