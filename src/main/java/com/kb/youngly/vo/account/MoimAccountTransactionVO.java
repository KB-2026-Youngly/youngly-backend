package com.kb.youngly.vo.account;

import com.kb.youngly.enums.TransactionCategory;
import com.kb.youngly.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code moim_account_transactions} 테이블의 모임통장 거래 원장 VO.
 *
 * <p>예치금 납부, 재충전, 정산, 환불의 거래 목적과 대상 참여자를 함께 보관한다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoimAccountTransactionVO {
    /** 모임통장 거래 식별자. */
    private Long moimAccountTransactionId;
    /** 잔액이 변경된 모임통장 ID. */
    private String moimAccountId;
    /** 거래 대상 그룹 참여자 ID. */
    private Long groupUserId;
    /** 거래가 특정 라운드에 속할 경우의 라운드 ID. */
    private Long roundId;
    /** 돈을 출금하거나 입금받는 개인 계좌 ID. */
    private String accountId;
    /** 모임통장 기준 입금·출금 방향. */
    private TransactionType transactionType;
    /** 최초 예치·재충전·정산·환불 중 거래 목적. */
    private TransactionCategory transactionCategory;
    /** 실제 이동한 금액. */
    private BigDecimal amount;
    /** 거래 반영 직후 모임통장 잔액. */
    private BigDecimal balanceAfter;
    /** 같은 요청의 중복 처리 방지를 위한 고유 키. */
    private String idempotencyKey;
    /** 사용자가 읽을 수 있는 거래 설명. */
    private String description;
    /** 거래 생성 시각. */
    private LocalDateTime createdAt;
}
