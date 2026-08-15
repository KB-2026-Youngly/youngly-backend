package com.kb.youngly.enums;

/**
 * KB 송금 요청의 처리 상태.
 *
 * <p>{@code FAILED}는 잔액 부족처럼 송금이 수행되지 않았음이 명확한 경우이고,
 * {@code UNKNOWN}은 타임아웃이나 연결 종료로 KB가 실제 송금을 수행했는지 즉시
 * 확정할 수 없는 경우다. UNKNOWN 요청은 같은 요청을 바로 재송금하지 않고
 * 멱등성 키 또는 KB 거래번호로 처리 결과를 먼저 조회해야 한다.</p>
 */
public enum KbTransferStatus {
    PENDING,
    SUCCESS,
    FAILED,
    UNKNOWN
}
