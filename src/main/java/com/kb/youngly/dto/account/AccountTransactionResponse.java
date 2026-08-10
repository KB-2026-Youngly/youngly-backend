package com.kb.youngly.dto.account;

import com.kb.youngly.enums.TransactionCategory;
import com.kb.youngly.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountTransactionResponse {

    private Long transactionId;
    private TransactionType transactionType;
    private TransactionCategory transactionCategory;
    private BigDecimal amount;
    private BigDecimal balanceAfter;

    private String description;
    private String anotherAccountNumber;
    private String anotherBankName;
    private String anotherName;
    private Long roundId;
    private Integer roundNo;
    private LocalDateTime createdAt;
}