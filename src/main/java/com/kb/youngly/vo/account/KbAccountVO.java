package com.kb.youngly.vo.account;

import com.kb.youngly.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * kb_accounts 테이블의 국민은행 계좌 정보.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbAccountVO {
    private String kbAccountId;
    private AccountType accountType;
    private String accountNumber;
    private String bankName;
    private BigDecimal balance;
    private BigDecimal interestRate;
    private String name;
    private LocalDateTime birthday;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
