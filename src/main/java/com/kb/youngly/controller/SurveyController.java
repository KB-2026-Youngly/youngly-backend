package com.kb.youngly.controller;

import com.kb.youngly.dto.survey.SurveyQuestionDTO;
import com.kb.youngly.dto.survey.SurveyResultDTO;
import com.kb.youngly.dto.survey.SurveySubmitRequestDTO;
import com.kb.youngly.dto.survey.SurveySubmitResponseDTO;
import com.kb.youngly.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    /**
     * 설문 문항/선택지 조회 (공개 API).
     * GET /api/surveys/questions
     */
    @GetMapping("/questions")
    public ResponseEntity<List<SurveyQuestionDTO>> getSurveyQuestions() {
        return ResponseEntity.ok(surveyService.getSurveyQuestionsWithChoices());
    }

    /**
     * 설문 응답 제출 + Baseline 산정 (인증 필요).
     * POST /api/surveys/results
     */
    @PostMapping("/results")
    public ResponseEntity<SurveySubmitResponseDTO> submitSurvey(
            Authentication authentication,
            @RequestBody SurveySubmitRequestDTO request) {

        String userId = authentication.getName();
        SurveySubmitResponseDTO response = surveyService.submitSurvey(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 로그인 사용자의 최신 설문 결과 조회 (인증 필요).
     * GET /api/surveys/results/latest
     */
    @GetMapping("/results/latest")
    public ResponseEntity<SurveyResultDTO> getLatestSurveyResult(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(surveyService.getLatestSurveyResult(userId));
    }
}
