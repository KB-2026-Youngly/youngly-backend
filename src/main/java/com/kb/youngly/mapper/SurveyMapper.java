package com.kb.youngly.mapper;

import com.kb.youngly.dto.auth.InterestOptionDTO;
import com.kb.youngly.dto.survey.SurveyQuestionDTO;
import com.kb.youngly.vo.survey.SurveyResultVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SurveyMapper {

    List<SurveyQuestionDTO> selectSurveyQuestionsWithChoices();

    int insertSurveyResult(SurveyResultVO surveyResultVO);

    SurveyResultVO selectLatestResultByUserId(@Param("userId") String userId);

    int insertSurveyResultInterest(@Param("surveyResultId") Long surveyResultId,
                                   @Param("interestIds") List<Long> interestIds);

    List<InterestOptionDTO> selectInterestsBySurveyResultId(@Param("surveyResultId") Long surveyResultId);
}