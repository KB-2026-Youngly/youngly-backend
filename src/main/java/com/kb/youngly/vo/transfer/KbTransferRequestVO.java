package com.kb.youngly.vo.transfer;

import com.kb.youngly.enums.KbTransferStatus;
import com.kb.youngly.enums.TransactionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** {@code kb_transfer_requests} 테이블의 한 건과 대응하는 송금 요청 VO. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbTransferRequestVO {

    private Long kbTransferRequestId;
    private String idempotencyKey;
    private String sourceKbAccountId;
    private String destinationKbAccountId;
    private BigDecimal amount;
    private BigDecimal sourceBalanceAfter;
    private BigDecimal destinationBalanceAfter;
    private TransactionCategory transactionCategory;
    private KbTransferStatus transferStatus;
    private String kbTransactionId;
    private String failureCode;
    private String failureMessage;
    private Long groupUserId;
    /** 정산 수령자의 user_id. DB 컬럼명 settlement_receiver_id와 대응한다. */
    private String settlementReceiverId;
    private Long roundId;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
}
