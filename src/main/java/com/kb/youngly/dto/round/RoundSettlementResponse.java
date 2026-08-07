package com.kb.youngly.dto.round;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundSettlementResponse {

    private Long settlementId;
    private String userId;
    private BigDecimal settlementAmount;
    private LocalDateTime createdAt;
}