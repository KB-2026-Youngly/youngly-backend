package com.kb.youngly.vo.user;

import com.kb.youngly.enums.Baseline;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationVO {
    private Long recommendationId;
    private String userId;
    private Baseline baseline;
    private String savingsPlan;
    private String financialProduct;
    private String investmentPortfolio;
    private String recommendationReason;
    private String referencedRoundRange;
    private LocalDateTime createdAt;
}
