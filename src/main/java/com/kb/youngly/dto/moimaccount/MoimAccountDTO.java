package com.kb.youngly.dto.moimaccount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoimAccountDTO {

    private String moimAccountId;

    private String accountNumber;

    private String bankName;

    private BigDecimal balance;

    private String accountName;

}