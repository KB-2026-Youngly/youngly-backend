package com.kb.youngly.vo.account;

import com.kb.youngly.enums.TransactionType;
import com.kb.youngly.enums.TransactionCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountTransactionVO {

    private Long accountTransactionId;
    private String kbAccountId;
    private Long groupUserId;
    private Long roundId;
    private TransactionType transactionType;
    private TransactionCategory transactionCategory;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String idempotencyKey;
    private String description;
    private String anotherAccountNumber;
    private String anotherBankName;
    private String anotherName;
    private LocalDateTime createdAt;
    private Integer roundNo;
}