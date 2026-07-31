package com.kb.youngly.vo.account;

import com.kb.youngly.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVO {
    private String accountId;
    private String userId;
    private String kbAccountId;
    private LocalDateTime createdAt;
    private AccountStatus accountStatus;
    private String accountName;
    private LocalDateTime updatedAt;
}
