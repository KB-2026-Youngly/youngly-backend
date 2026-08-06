package com.kb.youngly.dto.round;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundUserResponse {

    private Long roundUserId;

    private String userId;

    private String nickname;

    private Integer successCount;
}