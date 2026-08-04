package com.kb.youngly.service;

import com.kb.youngly.enums.PointType;
import com.kb.youngly.support.InMemoryPointMapper;
import com.kb.youngly.vo.point.PointHistoryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PointServiceTest {

    private static final String USER_ID = "user-id-123";

    private InMemoryPointMapper pointMapper;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        pointMapper = new InMemoryPointMapper();
        pointMapper.addUser(USER_ID, 1_000L);
        pointService = new PointService(pointMapper);
    }

    @Test
    @DisplayName("포인트 지급 시 잔액 증가와 EARN 내역 저장을 함께 수행한다")
    void earnPoint_updatesBalanceAndStoresHistory() {
        PointHistoryVO history = pointService.earnPoint(USER_ID, 120, "챌린지 인증 보상");

        assertEquals(1_120L, pointMapper.getBalance(USER_ID));
        assertEquals(1, pointMapper.getIncreaseCallCount());
        assertEquals(1, pointMapper.getInsertCallCount());
        assertNotNull(history.getPointLogId());
        assertEquals(USER_ID, history.getUserId());
        assertEquals(PointType.EARN, history.getPointType());
        assertEquals(120, history.getAmount());
        assertEquals("챌린지 인증 보상", history.getContent());
    }

    @Test
    @DisplayName("0 이하의 포인트는 잔액과 내역을 변경하지 않고 거부한다")
    void earnPoint_rejectsNonPositiveAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> pointService.earnPoint(USER_ID, 0, "잘못된 지급")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> pointService.earnPoint(USER_ID, -10, "잘못된 지급")
        );

        assertEquals(1_000L, pointMapper.getBalance(USER_ID));
        assertEquals(0, pointMapper.getIncreaseCallCount());
        assertEquals(0, pointMapper.getInsertCallCount());
    }

    @Test
    @DisplayName("10의 배수가 아닌 지급·사용 금액을 거부한다")
    void pointChange_rejectsAmountThatIsNotMultipleOfTen() {
        IllegalArgumentException earnException = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.earnPoint(USER_ID, 15, "잘못된 지급")
        );
        IllegalArgumentException useException = assertThrows(
                IllegalArgumentException.class,
                () -> pointService.usePoint(USER_ID, 25, "잘못된 사용")
        );

        assertEquals("포인트는 10의 배수여야 합니다.", earnException.getMessage());
        assertEquals("포인트는 10의 배수여야 합니다.", useException.getMessage());
        assertEquals(1_000L, pointMapper.getBalance(USER_ID));
        assertEquals(0, pointMapper.getIncreaseCallCount());
        assertEquals(0, pointMapper.getDecreaseCallCount());
        assertEquals(0, pointMapper.getInsertCallCount());
    }

    @Test
    @DisplayName("현재 포인트를 조회하고 존재하지 않는 사용자를 구분한다")
    void getPoint_returnsBalanceAndRejectsUnknownUser() {
        assertEquals(1_000L, pointService.getPoint(USER_ID));
        assertThrows(
                IllegalArgumentException.class,
                () -> pointService.getPoint("unknown-user")
        );
    }

    @Test
    @DisplayName("내역 조회 기본 페이징과 범위를 검증한다")
    void getPointHistories_normalizesAndValidatesPaging() {
        pointService.earnPoint(USER_ID, 100, "첫 번째");
        pointService.earnPoint(USER_ID, 200, "두 번째");

        List<PointHistoryVO> histories =
                pointService.getPointHistories(USER_ID, null, null);

        assertEquals(2, histories.size());
        assertEquals("두 번째", histories.get(0).getContent());
        assertEquals(USER_ID, pointMapper.getLastHistoryUserId());
        assertEquals(20, pointMapper.getLastHistoryLimit());
        assertEquals(0, pointMapper.getLastHistoryOffset());
        assertThrows(
                IllegalArgumentException.class,
                () -> pointService.getPointHistories(USER_ID, 0, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> pointService.getPointHistories(USER_ID, 101, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> pointService.getPointHistories(USER_ID, 20, -1)
        );
    }

    @Test
    @DisplayName("포인트 지급 메서드에 쓰기 트랜잭션이 선언되어 있다")
    void earnPoint_isTransactional() throws NoSuchMethodException {
        Method earnPoint = PointService.class.getMethod(
                "earnPoint",
                String.class,
                int.class,
                String.class
        );

        Transactional transactional = earnPoint.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }

    @Test
    @DisplayName("포인트 내역이 저장되지 않으면 예외를 발생시켜 트랜잭션 롤백 대상으로 만든다")
    void earnPoint_throwsWhenHistoryIsNotStored() {
        pointMapper.setInsertResult(0);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> pointService.earnPoint(USER_ID, 100, "저장 실패")
        );

        assertEquals("포인트 내역 저장에 실패했습니다.", exception.getMessage());
        assertEquals(1, pointMapper.getIncreaseCallCount());
        assertEquals(1, pointMapper.getInsertCallCount());
    }
}
