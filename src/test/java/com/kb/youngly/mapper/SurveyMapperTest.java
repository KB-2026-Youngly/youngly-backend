package com.kb.youngly.mapper;

import com.kb.youngly.dto.survey.SurveyQuestionDTO;
import com.kb.youngly.vo.survey.SurveyResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "file:src/main/webapp/WEB-INF/spring/root-context.xml"
})
class SurveyMapperTest {

    @Autowired
    private SurveyMapper surveyMapper;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUpTestData() throws Exception {
        try (Connection conn = dataSource.getConnection()) {

            insertTestUser(
                    conn,
                    "test_user_01",
                    "설문테스트유저1",
                    "login_test_user_01",
                    "nick_u01",
                    "test01@example.com",
                    "encoded-password-01"
            );

            insertTestUser(
                    conn,
                    "test_user_02",
                    "설문테스트유저2",
                    "login_test_user_02",
                    "nick_u02",
                    "test02@example.com",
                    "encoded-password-02"
            );
        }
    }

    /**
     * users 테이블에 테스트용 부모 데이터를 넣는다.
     * survey_results.user_id FK 때문에 반드시 users에 먼저 존재해야 한다.
     */
    private void insertTestUser(
            Connection conn,
            String userId,
            String name,
            String loginId,
            String nickname,
            String email,
            String password
    ) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (" +
                        "user_id, name, login_id, nickname, email, password, user_status, point" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "name = VALUES(name), " +
                        "password = VALUES(password), " +
                        "user_status = VALUES(user_status), " +
                        "point = VALUES(point)"
        )) {
            ps.setString(1, userId);
            ps.setString(2, name);
            ps.setString(3, loginId);
            ps.setString(4, nickname);
            ps.setString(5, email);
            ps.setString(6, password);
            ps.setString(7, "ACTIVE");
            ps.setLong(8, 0L);
            ps.executeUpdate();
        }
    }

    @Test
    @DisplayName("설문 문항과 선택지를 조회할 수 있다")
    void selectSurveyQuestionsWithChoices_success() {
        List<SurveyQuestionDTO> questions = surveyMapper.selectSurveyQuestionsWithChoices();

        assertNotNull(questions);
        assertFalse(questions.isEmpty());

        SurveyQuestionDTO firstQuestion = questions.get(0);
        assertNotNull(firstQuestion.getQuestionId());
        assertNotNull(firstQuestion.getQuestionText());
        assertNotNull(firstQuestion.getChoices());
        assertFalse(firstQuestion.getChoices().isEmpty());
    }

    @Test
    @DisplayName("설문 결과를 저장하고 최신 결과를 조회할 수 있다")
    void insertSurveyResult_and_selectLatestResultByUserId_success() {
        String userId = "test_user_01";

        SurveyResultVO surveyResult = new SurveyResultVO();
        surveyResult.setUserId(userId);
        surveyResult.setAnswersJson("[{\"questionId\":1,\"choiceId\":2}]");
        surveyResult.setTotalScore(10);
        surveyResult.setBaseline("적극적인 성장형");
        surveyResult.setSubmittedAt(LocalDateTime.now());
        surveyResult.setCalculatedAt(LocalDateTime.now());

        int insertCount = surveyMapper.insertSurveyResult(surveyResult);

        assertEquals(1, insertCount);
        assertNotNull(surveyResult.getSurveyResultId());
        assertTrue(surveyResult.getSurveyResultId() > 0);

        SurveyResultVO latestResult = surveyMapper.selectLatestResultByUserId(userId);

        assertNotNull(latestResult);
        assertEquals(surveyResult.getSurveyResultId(), latestResult.getSurveyResultId());
        assertEquals(userId, latestResult.getUserId());
        assertEquals(surveyResult.getTotalScore(), latestResult.getTotalScore());
        assertEquals(surveyResult.getBaseline(), latestResult.getBaseline());
    }
}
