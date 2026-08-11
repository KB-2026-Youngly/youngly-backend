package com.kb.youngly.dto.moimaccount;

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
public class MoimAccountBalanceSyncResponseDTO {

    private String moimAccountId;

    private BigDecimal balance;

    private LocalDateTime syncedAt;
}
