package com.kb.youngly.dto.round;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundRankingResponse {

    private String userId;
    private String nickname;
    private Integer rankNo;
    private Integer successCount;
}