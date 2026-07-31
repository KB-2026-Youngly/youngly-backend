package com.kb.youngly.service;

import com.kb.youngly.enums.PointType;
import com.kb.youngly.mapper.PointMapper;
import com.kb.youngly.vo.point.PointHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자의 포인트 조회, 적립, 사용 및 내역 조회를 담당하는 서비스입니다.
 * 입력값을 검증한 뒤 포인트 잔액 변경과 내역 저장을 하나의 트랜잭션으로 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class PointService {
    // 조회 개수를 입력하지 않았을 때 적용할 기본값과 한 번에 조회할 수 있는 최대값입니다.
    private static final int DEFAULT_HISTORY_LIMIT = 20;
    private static final int MAX_HISTORY_LIMIT = 100;

    private final PointMapper pointMapper;

    /**
     * 사용자의 현재 보유 포인트를 조회합니다.
     * 사용자 ID가 비어 있거나 존재하지 않으면 오류가 발생합니다.
     */
    @Transactional(readOnly = true)
    public long getPoint(String userId) {
        Long point = pointMapper.findUserPoint(requireUserId(userId));
        if (point == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        return point;
    }

    /**
     * 사용자의 포인트 적립·사용 내역을 최신순으로 조회합니다.
     * limit은 한 번에 조회할 개수이고, offset은 처음부터 건너뛸 개수입니다.
     * 값을 입력하지 않으면 각각 20과 0을 사용합니다.
     */
    @Transactional(readOnly = true)
    public List<PointHistoryVO> getPointHistories(String userId, Integer limit, Integer offset) {
        String validUserId = requireUserId(userId);
        int validLimit = normalizeLimit(limit);
        int validOffset = normalizeOffset(offset);
        return pointMapper.findPointHistoriesByUserId(validUserId, validLimit, validOffset);
    }

    /**
     * 상품과 관계없는 일반 포인트를 적립합니다.
     */
    @Transactional
    public PointHistoryVO earnPoint(String userId, int amount, String content) {
        return earnPoint(userId, null, amount, content);
    }

    /**
     * 포인트를 적립하고 적립 내역을 저장합니다.
     * 잔액 변경이나 내역 저장 중 하나라도 실패하면 전체 작업이 롤백됩니다.
     * 상품과 관련 없는 적립이라면 itemId에는 null이 들어갑니다.
     */
    @Transactional
    public PointHistoryVO earnPoint(String userId, Long itemId, int amount, String content) {
        PointHistoryVO history = buildHistory(userId, itemId, PointType.EARN, amount, content);

        // UPDATE 결과가 0이면 해당 사용자가 존재하지 않는다는 뜻입니다.
        int updatedRows = pointMapper.increaseUserPoint(history.getUserId(), amount);
        if (updatedRows != 1) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        pointMapper.insertPointHistory(history);
        return history;
    }

    /**
     * 상품과 관계없는 일반 포인트를 사용합니다.
     */
    @Transactional
    public PointHistoryVO usePoint(String userId, int amount, String content) {
        return usePoint(userId, null, amount, content);
    }

    /**
     * 포인트를 차감하고 사용 내역을 저장합니다.
     * 잔액 변경이나 내역 저장 중 하나라도 실패하면 전체 작업이 롤백됩니다.
     * 상품과 관련 없는 사용이라면 itemId에는 null이 들어갑니다.
     */
    @Transactional
    public PointHistoryVO usePoint(String userId, Long itemId, int amount, String content) {
        PointHistoryVO history = buildHistory(userId, itemId, PointType.USE, amount, content);

        // SQL에서 잔액을 함께 확인하므로 포인트가 부족하면 차감되지 않고 0을 반환합니다.
        int updatedRows = pointMapper.decreaseUserPoint(history.getUserId(), amount);
        if (updatedRows != 1) {
            throw new IllegalArgumentException("포인트가 부족하거나 존재하지 않는 사용자입니다.");
        }

        pointMapper.insertPointHistory(history);
        return history;
    }

    /**
     * 검증된 값으로 적립 또는 사용 내역 객체를 만듭니다.
     */
    private PointHistoryVO buildHistory(String userId,
                                        Long itemId,
                                        PointType pointType,
                                        int amount,
                                        String content) {
        validateAmount(amount);
        return PointHistoryVO.builder()
                .userId(requireUserId(userId))
                .itemId(itemId)
                .pointType(pointType)
                .amount(amount)
                .content(normalizeContent(content))
                .build();
    }

    /**
     * 사용자 ID가 입력되었는지 확인합니다.
     */
    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        return userId;
    }

    /**
     * 적립하거나 사용할 포인트가 양수인지 확인합니다.
     */
    private void validateAmount(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("포인트는 1 이상이어야 합니다.");
        }
    }

    /**
     * 빈 내용은 {@code null}로 바꾸고 최대 길이를 검사합니다.
     */
    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        if (content.length() > 100) {
            throw new IllegalArgumentException("포인트 내역 내용은 100자를 초과할 수 없습니다.");
        }
        return content;
    }

    /**
     * 내역 조회 개수를 기본값으로 보정하고 허용 범위를 검사합니다.
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_HISTORY_LIMIT;
        }
        if (limit < 1 || limit > MAX_HISTORY_LIMIT) {
            throw new IllegalArgumentException("조회 개수는 1 이상 100 이하이어야 합니다.");
        }
        return limit;
    }

    /**
     * 조회 시작 위치를 기본값으로 보정하고 음수인지 검사합니다.
     */
    private int normalizeOffset(Integer offset) {
        if (offset == null) {
            return 0;
        }
        if (offset < 0) {
            throw new IllegalArgumentException("조회 시작 위치는 0 이상이어야 합니다.");
        }
        return offset;
    }
}
