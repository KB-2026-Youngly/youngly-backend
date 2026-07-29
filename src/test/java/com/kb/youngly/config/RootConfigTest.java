package com.kb.youngly.config;

import lombok.extern.log4j.Log4j2;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 초기 세팅 검증 테스트.
 *
 * <p>[WARN] 이 테스트는 로컬 MySQL 이 기동 중이고 youngly_db 스키마와 계정이
 * 준비되어 있어야 통과한다. 실행 전 db/schema.sql 을 먼저 적용할 것.</p>
 *
 * <p>[INFO] 실행 : ./gradlew test --tests "com.kb.youngly.config.RootConfigTest"</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class})
@Log4j2
class RootConfigTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    @DisplayName("DataSource 커넥션이 정상적으로 연결된다")
    void dataSourceConnection() {
        try (Connection con = dataSource.getConnection()) {
            assertNotNull(con);
            log.info("[INFO] DataSource 연결 성공 : {}", con);
        } catch (Exception e) {
            fail("[ERROR] DataSource 연결 실패 : " + e.getMessage());
        }
    }

    @Test
    @DisplayName("SqlSessionFactory 로 세션을 열 수 있다")
    void sqlSessionFactory() {
        try (SqlSession session = sqlSessionFactory.openSession();
             Connection con = session.getConnection()) {
            assertNotNull(session);
            assertNotNull(con);
            log.info("[INFO] SqlSession 생성 성공 : {}", session);
        } catch (Exception e) {
            fail("[ERROR] SqlSession 생성 실패 : " + e.getMessage());
        }
    }
}
