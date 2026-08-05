package com.kb.youngly.service;

import com.kb.youngly.dto.character.CharacterDrawResponse;
import com.kb.youngly.mapper.CharacterMapper;
import com.kb.youngly.mapper.PointMapper;
import com.kb.youngly.vo.character.CharacterEquipVO;
import com.kb.youngly.vo.character.OwnedCharacterVO;
import com.kb.youngly.vo.point.CollectibleItemVO;
import com.kb.youngly.vo.point.UserItemVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CharacterTransactionIntegrationTest.TestConfig.class)
class CharacterTransactionIntegrationTest {

    @Configuration
    @ImportResource("file:src/main/webapp/WEB-INF/spring/root-context.xml")
    static class TestConfig {

        @Bean
        PointService pointService(PointMapper pointMapper) {
            return new PointService(pointMapper);
        }

        @Bean
        CharacterService characterService(CharacterMapper characterMapper,
                                          PointService pointService) {
            return new CharacterService(characterMapper, pointService);
        }
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CharacterMapper characterMapper;

    @Autowired
    private PointService pointService;

    @Autowired
    private CharacterService characterService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbcTemplate;
    private String userId;
    private Long testCharacterId;
    private int initiallyOwnedCharacterCount;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        userId = "gacha-" + suffix;

        jdbcTemplate.update(
                "INSERT INTO users "
                        + "(user_id, name, login_id, nickname, email, password, birthday, user_status, point) "
                        + "VALUES (?, ?, ?, ?, ?, ?, '2000-01-01', 'ACTIVE', ?)",
                userId,
                "가챠테스트",
                "g" + suffix,
                "가챠" + suffix,
                "gacha-" + suffix + "@test.local",
                "test-password",
                500L
        );

        // 기존 마스터 캐릭터는 모두 보유 처리해 테스트용 캐릭터 하나만 추첨 대상으로 만든다.
        List<Long> existingCharacterIds = jdbcTemplate.queryForList(
                "SELECT item_id FROM collectible_items WHERE item_category = 'CHARACTER'",
                Long.class
        );
        for (Long itemId : existingCharacterIds) {
            jdbcTemplate.update(
                    "INSERT INTO user_items (item_id, user_id, is_equipped) VALUES (?, ?, false)",
                    itemId,
                    userId
            );
        }
        initiallyOwnedCharacterCount = existingCharacterIds.size();

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("item_category", "CHARACTER");
        values.put("item_name", "통합테스트 캐릭터 " + suffix);
        values.put("image_url", "/test/characters/" + suffix + ".png");
        values.put("drop_rate", BigDecimal.ZERO);
        values.put("base_character", "ETC");
        testCharacterId = new SimpleJdbcInsert(dataSource)
                .withTableName("collectible_items")
                .usingColumns(
                        "item_category",
                        "item_name",
                        "image_url",
                        "drop_rate",
                        "base_character"
                )
                .usingGeneratedKeyColumns("item_id")
                .executeAndReturnKey(values)
                .longValue();
    }

    @AfterEach
    void cleanUp() {
        if (jdbcTemplate == null || userId == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM point_history WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_items WHERE user_id = ?", userId);
        if (testCharacterId != null) {
            jdbcTemplate.update(
                    "DELETE FROM collectible_items WHERE item_id = ?",
                    testCharacterId
            );
        }
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", userId);
    }

    @Test
    @DisplayName("같은 사용자의 동시 뽑기는 직렬화되어 중복 획득과 중복 차감을 막는다")
    void concurrentDraw_serializesSameUserRequests() throws Exception {
        jdbcTemplate.update("UPDATE users SET point = 200 WHERE user_id = ?", userId);
        assertTrue(AopUtils.isAopProxy(characterService));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Object> draw = () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 요청 시작 대기 시간이 초과되었습니다.");
            }
            try {
                return characterService.drawCharacter(userId);
            } catch (RuntimeException e) {
                return e;
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Object> first = executor.submit(draw);
            Future<Object> second = executor.submit(draw);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            Object firstResult = first.get(10, TimeUnit.SECONDS);
            Object secondResult = second.get(10, TimeUnit.SECONDS);
            long successCount = List.of(firstResult, secondResult).stream()
                    .filter(CharacterDrawResponse.class::isInstance)
                    .count();
            long allOwnedFailureCount = List.of(firstResult, secondResult).stream()
                    .filter(IllegalArgumentException.class::isInstance)
                    .map(IllegalArgumentException.class::cast)
                    .filter(exception -> "모든 캐릭터를 이미 보유하고 있습니다."
                            .equals(exception.getMessage()))
                    .count();

            assertEquals(1, successCount);
            assertEquals(1, allOwnedFailureCount);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(
                100L,
                jdbcTemplate.queryForObject(
                        "SELECT point FROM users WHERE user_id = ?",
                        Long.class,
                        userId
                )
        );
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM user_items WHERE user_id = ? AND item_id = ?",
                        Integer.class,
                        userId,
                        testCharacterId
                )
        );
        assertEquals(
                initiallyOwnedCharacterCount + 1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM user_items WHERE user_id = ?",
                        Integer.class,
                        userId
                )
        );
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM point_history "
                                + "WHERE user_id = ? AND item_id = ? "
                                + "AND point_type = 'USE' AND amount = 100",
                        Integer.class,
                        userId,
                        testCharacterId
                )
        );
    }

    @Test
    @DisplayName("보유 정보 저장 실패 시 포인트 차감과 사용 내역도 함께 롤백한다")
    void ownershipInsertFailure_rollsBackPointAndHistory() {
        CharacterMapper failingMapper = new FailingInsertCharacterMapper(characterMapper);
        CharacterService failingService = new CharacterService(
                failingMapper,
                pointService,
                bound -> 0
        );
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> transactionTemplate.executeWithoutResult(
                        status -> failingService.drawCharacter(userId)
                )
        );

        assertEquals("캐릭터 보유 정보 저장에 실패했습니다.", exception.getMessage());
        assertEquals(
                500L,
                jdbcTemplate.queryForObject(
                        "SELECT point FROM users WHERE user_id = ?",
                        Long.class,
                        userId
                )
        );
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM point_history WHERE user_id = ?",
                        Integer.class,
                        userId
                )
        );
        assertEquals(
                initiallyOwnedCharacterCount,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM user_items WHERE user_id = ?",
                        Integer.class,
                        userId
                )
        );
    }

    private static class FailingInsertCharacterMapper implements CharacterMapper {

        private final CharacterMapper delegate;

        private FailingInsertCharacterMapper(CharacterMapper delegate) {
            this.delegate = delegate;
        }

        @Override
        public String lockUserForUpdate(String userId) {
            return delegate.lockUserForUpdate(userId);
        }

        @Override
        public List<CollectibleItemVO> findUnownedCharacters(String userId) {
            return delegate.findUnownedCharacters(userId);
        }

        @Override
        public int insertUserItem(UserItemVO userItem) {
            return 0;
        }

        @Override
        public List<OwnedCharacterVO> findOwnedCharacters(String userId) {
            return delegate.findOwnedCharacters(userId);
        }

        @Override
        public CharacterEquipVO findOwnedCharacterForEquip(String userId, Long characterId) {
            return delegate.findOwnedCharacterForEquip(userId, characterId);
        }

        @Override
        public int unequipOtherCharacters(String userId, Long characterId) {
            return delegate.unequipOtherCharacters(userId, characterId);
        }

        @Override
        public int equipCharacter(String userId, Long characterId) {
            return delegate.equipCharacter(userId, characterId);
        }
    }
}
