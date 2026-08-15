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

    Integer selectChoiceScore(@Param("questionNo") int questionNo,
                              @Param("choiceId") long choiceId);

    int countChoiceByQuestionNoAndChoiceId(@Param("questionNo") int questionNo,
                                           @Param("choiceId") long choiceId);

    List<InterestOptionDTO> selectAllInterests();

    SurveyResultVO selectBySurveyResultIdAndUserId(@Param("surveyResultId") Long surveyResultId,
                                                   @Param("userId") String userId);
}
