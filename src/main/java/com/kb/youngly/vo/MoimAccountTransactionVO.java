package com.kb.youngly.vo;

import com.kb.youngly.enums.TransactionCategory;
import com.kb.youngly.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoimAccountTransactionVO {
    private Long moimAccountTransactionId;
    private String moimAccountId;
    private Long groupUserId;
    private Long roundId;
    private String accountId;
    private TransactionType transactionType;
    private TransactionCategory transactionCategory;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String idempotencyKey;
    private String description;
    private LocalDateTime createdAt;
}
