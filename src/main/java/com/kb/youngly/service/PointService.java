package com.kb.youngly.service;

import com.kb.youngly.enums.PointType;
import com.kb.youngly.mapper.PointMapper;
import com.kb.youngly.vo.PointHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {
    private static final int DEFAULT_HISTORY_LIMIT = 20;
    private static final int MAX_HISTORY_LIMIT = 100;

    private final PointMapper pointMapper;

    @Transactional(readOnly = true)
    public long getPoint(String userId) {
        Long point = pointMapper.findUserPoint(requireUserId(userId));
        if (point == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        return point;
    }

    @Transactional(readOnly = true)
    public List<PointHistoryVO> getPointHistories(String userId, Integer limit, Integer offset) {
        String validUserId = requireUserId(userId);
        int validLimit = normalizeLimit(limit);
        int validOffset = normalizeOffset(offset);
        return pointMapper.findPointHistoriesByUserId(validUserId, validLimit, validOffset);
    }

    @Transactional
    public PointHistoryVO earnPoint(String userId, int amount, String content) {
        return earnPoint(userId, null, amount, content);
    }

    @Transactional
    public PointHistoryVO earnPoint(String userId, Long itemId, int amount, String content) {
        PointHistoryVO history = buildHistory(userId, itemId, PointType.EARN, amount, content);
        int updatedRows = pointMapper.increaseUserPoint(history.getUserId(), amount);
        if (updatedRows != 1) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        pointMapper.insertPointHistory(history);
        return history;
    }

    @Transactional
    public PointHistoryVO usePoint(String userId, int amount, String content) {
        return usePoint(userId, null, amount, content);
    }

    @Transactional
    public PointHistoryVO usePoint(String userId, Long itemId, int amount, String content) {
        PointHistoryVO history = buildHistory(userId, itemId, PointType.USE, amount, content);
        int updatedRows = pointMapper.decreaseUserPoint(history.getUserId(), amount);
        if (updatedRows != 1) {
            throw new IllegalArgumentException("포인트가 부족하거나 존재하지 않는 사용자입니다.");
        }
        pointMapper.insertPointHistory(history);
        return history;
    }

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

    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        return userId;
    }

    private void validateAmount(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("포인트는 1 이상이어야 합니다.");
        }
    }

    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        if (content.length() > 100) {
            throw new IllegalArgumentException("포인트 내역 내용은 100자를 초과할 수 없습니다.");
        }
        return content;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_HISTORY_LIMIT;
        }
        if (limit < 1 || limit > MAX_HISTORY_LIMIT) {
            throw new IllegalArgumentException("조회 개수는 1 이상 100 이하이어야 합니다.");
        }
        return limit;
    }

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
