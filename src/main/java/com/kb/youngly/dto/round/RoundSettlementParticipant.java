package com.kb.youngly.dto.round;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 라운드 정산 한 건에 필요한 참여자, 계좌, 예치금 정보를 한 번에 전달한다.
 *
 * <p>서비스는 이 DTO의 행들을 잠근 상태에서 처리하여 정산 도중 예치금이나
 * 계좌 연결 정보가 바뀌어 원장과 실제 잔액이 어긋나는 일을 막는다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundSettlementParticipant {
    private Long roundHistoryId;
    private String userId;
    private String accountId;
    private String moimAccountId;
    private Integer rankNo;
    private Long groupUserId;
    private BigDecimal currentDepositAmount;
    private String destinationKbAccountId;
    private String destinationAccountNumber;
    private String destinationAccountName;
    private String sourceKbAccountId;
    private String sourceAccountNumber;
    private String sourceAccountName;
}
