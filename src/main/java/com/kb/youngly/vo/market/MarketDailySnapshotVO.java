package com.kb.youngly.vo.market;

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
public class MarketDailySnapshotVO {

    private Long id;
    private LocalDate marketDate;
    private String sourceSubject;
    private String pdfFileName;
    private String pdfUrl;
    private String rawText;
    private String summaryText;
    private String marketHeadlineText;
    private String marketDetailText;
    private BigDecimal kospi;
    private BigDecimal kosdaq;
    private BigDecimal nasdaq;
    private BigDecimal usdkrw;
    private BigDecimal treasury3y;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
