package com.kb.youngly.dto.account;

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
public class AccountBalanceSyncResponseDTO {

    private String accountId;

    private BigDecimal balance;

    private LocalDateTime syncedAt;
}
