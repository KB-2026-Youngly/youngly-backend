package com.kb.youngly.dto.transfer;

import com.kb.youngly.enums.TransactionCategory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Youngly 업무 서비스가 KB 송금 서비스에 전달하는 한 건의 송금 명령.
 *
 * <p>계좌와 금액뿐 아니라 멱등성 키와 업무 식별자를 함께 전달하여 송금 요청을
 * 라운드·참여자와 연결하고, 배치가 재실행되더라도 동일 송금이 중복 실행되지 않게 한다.</p>
 */
@Getter
@Builder
public class KbTransferCommand {

    private final String idempotencyKey;
    private final String sourceKbAccountId;
    private final String destinationKbAccountId;
    private final BigDecimal amount;
    private final TransactionCategory transactionCategory;
    /**
     * CHARGE에서는 송금 참여자, SETTLEMENT에서는 실패 내역을 조회할 그룹장의 group_user_id.
     */
    private final Long groupUserId;
    /** SETTLEMENT에서 실제 정산금을 받는 round_history.user_id. */
    private final String settlementReceiverId;
    private final Long roundId;
}
