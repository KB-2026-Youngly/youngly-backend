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
import java.sql.Date;
import java.sql.Statement;
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
            seedSurveyMasterData(conn);
        }
    }

    private void seedSurveyMasterData(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM survey_choices");
            stmt.executeUpdate("DELETE FROM survey_questions");
            stmt.executeUpdate(
                    "INSERT INTO survey_questions (question_id, question_no, question_text, is_multiple) VALUES " +
                            "(1, 1, '투자 경험이 있으신가요?', FALSE)," +
                            "(2, 2, '금융상품에 대해 얼마나 알고 계신가요?', FALSE)," +
                            "(3, 3, '돈을 모으는 목적은 무엇인가요?', FALSE)," +
                            "(4, 4, '수익과 안정성 중 무엇이 더 중요한가요?', FALSE)," +
                            "(5, 5, '얼마 동안 모을 계획이신가요?', FALSE)," +
                            "(6, 6, '손실이 발생하면 어느 정도까지 감내할 수 있나요?', FALSE)"
            );
            stmt.executeUpdate(
                    "INSERT INTO survey_choices (choice_id, question_id, choice_text, score, display_order) VALUES " +
                            "(1, 1, '투자 경험 없음', 1, 1)," +
                            "(2, 1, '1년 미만', 2, 2)," +
                            "(3, 1, '1~2년', 3, 3)," +
                            "(4, 1, '2~3년', 4, 4)," +
                            "(5, 1, '3년 이상', 5, 5)," +
                            "(6, 2, '거의 모른다', 1, 1)," +
                            "(7, 2, '용어 정도는 안다', 2, 2)," +
                            "(8, 2, '기본 상품 구조를 이해한다', 3, 3)," +
                            "(9, 2, '수익률과 리스크를 비교할 수 있다', 4, 4)," +
                            "(10, 2, '전문적으로 분석할 수 있다', 5, 5)," +
                            "(11, 3, '비상금 마련', 1, 1)," +
                            "(12, 3, '단기 목돈 마련', 2, 2)," +
                            "(13, 3, '주택·결혼 등 중기 목적', 3, 3)," +
                            "(14, 3, '자산 증식', 4, 4)," +
                            "(15, 3, '공격적인 수익 창출', 5, 5)," +
                            "(16, 4, '무조건 안정성', 1, 1)," +
                            "(17, 4, '안정성이 더 중요', 2, 2)," +
                            "(18, 4, '반반', 3, 3)," +
                            "(19, 4, '수익성이 더 중요', 4, 4)," +
                            "(20, 4, '무조건 수익성', 5, 5)," +
                            "(21, 5, '6개월 이내', 1, 1)," +
                            "(22, 5, '6개월~1년', 2, 2)," +
                            "(23, 5, '1~3년', 3, 3)," +
                            "(24, 5, '3~5년', 4, 4)," +
                            "(25, 5, '5년 이상', 5, 5)," +
                            "(26, 6, '원금 손실은 원하지 않는다', 1, 1)," +
                            "(27, 6, '아주 작은 손실만 감내한다', 2, 2)," +
                            "(28, 6, '어느 정도 손실은 감내 가능하다', 3, 3)," +
                            "(29, 6, '변동성을 감수하고 수익을 추구한다', 4, 4)," +
                            "(30, 6, '큰 손실 가능성도 감수한다', 5, 5)"
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
                        "user_id, name, login_id, nickname, email, password, user_status, point, birthday" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "name = VALUES(name), " +
                        "password = VALUES(password), " +
                        "user_status = VALUES(user_status), " +
                        "point = VALUES(point), " +
                        "birthday = VALUES(birthday)"
        )) {
            ps.setString(1, userId);
            ps.setString(2, name);
            ps.setString(3, loginId);
            ps.setString(4, nickname);
            ps.setString(5, email);
            ps.setString(6, password);
            ps.setString(7, "ACTIVE");
            ps.setLong(8, 0L);
            ps.setDate(9, Date.valueOf("1990-01-01"));
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
        assertTrue(questions.stream().anyMatch(question ->
                question.getChoices() != null && !question.getChoices().isEmpty()));
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
