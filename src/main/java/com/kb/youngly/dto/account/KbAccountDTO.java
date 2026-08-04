package com.kb.youngly.dto.account;

import com.kb.youngly.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbAccountDTO {

    private String kbAccountId;

    private AccountType accountType;

    private String accountNumber;

    private String bankName;

    private BigDecimal balance;
}