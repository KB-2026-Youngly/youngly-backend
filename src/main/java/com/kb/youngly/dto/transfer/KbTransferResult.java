package com.kb.youngly.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 성공이 확정된 KB 송금 결과.
 *
 * <p>현재 Mock 구현은 실패 시 예외를 발생시키므로 이 객체가 반환됐다는 것 자체가
 * 출금과 입금이 모두 성공했다는 의미다. 두 잔액은 성공 당시 값이며 계좌별 거래원장의
 * {@code balance_after}를 정확하게 기록하는 데 사용한다.</p>
 */
@Getter
@AllArgsConstructor
public class KbTransferResult {

    private final Long kbTransferRequestId;
    private final String kbTransactionId;
    private final BigDecimal sourceBalanceAfter;
    private final BigDecimal destinationBalanceAfter;
}
