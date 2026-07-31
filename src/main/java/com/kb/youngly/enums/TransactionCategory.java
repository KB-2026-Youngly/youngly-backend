package com.kb.youngly.enums;

/**
 * 모임통장 거래가 발생한 업무 목적.
 *
 * <p>{@link TransactionType}이 모임통장 기준 입출금 방향을 나타낸다면,
 * 이 enum은 충전, 정산, 환불 중 어떤 업무로 발생한 거래인지
 * 구분한다.</p>
 */
public enum TransactionCategory {
    CHARGE,
    SETTLEMENT,
    REFUND
}
