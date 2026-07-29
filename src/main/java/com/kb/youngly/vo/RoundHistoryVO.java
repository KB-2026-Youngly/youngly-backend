package com.kb.youngly.vo;

import com.kb.youngly.enums.PriorFailureResponse;
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
public class RoundHistoryVO {
    private Long roundHistoryId;
    private Long roundId;
    private String userId;
    private String accountId;
    private String moimAccountId;
    private Integer rankNo;
    private Integer successCount;
    private BigDecimal settlementAmount;
    private Integer remainingFailPassCount;
    private PriorFailureResponse priorFailureResponse;
    private LocalDateTime createdAt;
    private LocalDateTime settlementAt;
}
