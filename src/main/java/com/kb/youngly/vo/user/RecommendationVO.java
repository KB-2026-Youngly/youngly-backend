package com.kb.youngly.vo.user;

import com.kb.youngly.enums.Baseline;
import com.kb.youngly.enums.GenerationMode;
import com.kb.youngly.enums.GuardrailStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationVO {
    private Long recommendationId;
    private String userId;
    private Long surveyResultId;
    private Baseline baseline;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private BigDecimal settledAmountThisMonth;
    private BigDecimal expectedAdditionalAmountCurrentRank;
    private BigDecimal expectedAdditionalAmountBestCase;
    private BigDecimal expectedTotalAmountThisMonth;
    private BigDecimal expectedMaxTotalAmountThisMonth;
    private BigDecimal currentPensionBalance;
    private Boolean hasOngoingRoundThisMonth;
    private LocalDate nextDepositDate;
    private String forecastBasisJson;
    private String marketHighlightsJson;
    private String marketDetail;
    private String pensionInsightIntro;
    private String pensionInsightStrategy;
    private GuardrailStatus guardrailStatus;
    private GenerationMode generationMode;
    private LocalDateTime createdAt;
}
