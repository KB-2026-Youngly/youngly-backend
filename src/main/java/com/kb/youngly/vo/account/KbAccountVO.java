package com.kb.youngly.vo.account;

import com.kb.youngly.enums.AccountType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private LocalDate birthday;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
