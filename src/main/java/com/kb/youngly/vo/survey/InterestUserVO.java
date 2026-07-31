package com.kb.youngly.vo.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestUserVO {
    private Long interestId;
    private String userId;
    private LocalDateTime createdAt;
}
