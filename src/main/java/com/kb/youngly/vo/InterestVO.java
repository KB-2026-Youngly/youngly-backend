package com.kb.youngly.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestVO {
    private Long interestId;
    private String interestName;
    private Boolean isInvestment;
    private LocalDateTime createdAt;
}
