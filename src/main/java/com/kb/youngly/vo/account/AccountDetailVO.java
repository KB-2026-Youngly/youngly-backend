package com.kb.youngly.vo.account;

import com.kb.youngly.enums.AccountStatus;
import com.kb.youngly.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDetailVO {

    private String accountId;

    private String userId;

    private String kbAccountId;

    private AccountType accountType;

    private AccountStatus accountStatus;

    private String accountName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}