package com.kb.youngly.support;

import com.kb.youngly.mapper.PointMapper;
import com.kb.youngly.vo.PointHistoryVO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryPointMapper implements PointMapper {

    private final Map<String, Long> balances = new HashMap<>();
    private final List<PointHistoryVO> histories = new ArrayList<>();
    private long nextPointLogId = 1L;
    private int increaseCallCount;
    private int decreaseCallCount;
    private int insertCallCount;
    private int insertResult = 1;
    private String lastHistoryUserId;
    private Integer lastHistoryLimit;
    private Integer lastHistoryOffset;

    public void addUser(String userId, long balance) {
        balances.put(userId, balance);
    }

    public long getBalance(String userId) {
        return balances.get(userId);
    }

    public List<PointHistoryVO> getHistories() {
        return new ArrayList<>(histories);
    }

    public int getIncreaseCallCount() {
        return increaseCallCount;
    }

    public int getDecreaseCallCount() {
        return decreaseCallCount;
    }

    public int getInsertCallCount() {
        return insertCallCount;
    }

    public void setInsertResult(int insertResult) {
        this.insertResult = insertResult;
    }

    public String getLastHistoryUserId() {
        return lastHistoryUserId;
    }

    public Integer getLastHistoryLimit() {
        return lastHistoryLimit;
    }

    public Integer getLastHistoryOffset() {
        return lastHistoryOffset;
    }

    @Override
    public Long findUserPoint(String userId) {
        return balances.get(userId);
    }

    @Override
    public int increaseUserPoint(String userId, int amount) {
        increaseCallCount++;
        Long balance = balances.get(userId);
        if (balance == null) {
            return 0;
        }
        balances.put(userId, balance + amount);
        return 1;
    }

    @Override
    public int decreaseUserPoint(String userId, int amount) {
        decreaseCallCount++;
        Long balance = balances.get(userId);
        if (balance == null || balance < amount) {
            return 0;
        }
        balances.put(userId, balance - amount);
        return 1;
    }

    @Override
    public int insertPointHistory(PointHistoryVO pointHistory) {
        insertCallCount++;
        if (insertResult != 1) {
            return insertResult;
        }
        pointHistory.setPointLogId(nextPointLogId++);
        pointHistory.setCreatedAt(LocalDateTime.now());
        histories.add(0, pointHistory);
        return 1;
    }

    @Override
    public List<PointHistoryVO> findPointHistoriesByUserId(
            String userId,
            int limit,
            int offset) {
        lastHistoryUserId = userId;
        lastHistoryLimit = limit;
        lastHistoryOffset = offset;

        List<PointHistoryVO> userHistories = histories.stream()
                .filter(history -> userId.equals(history.getUserId()))
                .toList();
        int fromIndex = Math.min(offset, userHistories.size());
        int toIndex = Math.min(fromIndex + limit, userHistories.size());
        return new ArrayList<>(userHistories.subList(fromIndex, toIndex));
    }
}
