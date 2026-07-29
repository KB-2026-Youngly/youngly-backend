package com.kb.youngly.vo;

import com.kb.youngly.enums.AccountStatus;
import com.kb.youngly.enums.AccountType;
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
public class AccountVO {
    private String accountId;
    private String userId;
    private AccountType accountType;
    private String accountNumber;
    private String bankName;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private AccountStatus accountStatus;
    private String accountName;
    private LocalDateTime updatedAt;
}
