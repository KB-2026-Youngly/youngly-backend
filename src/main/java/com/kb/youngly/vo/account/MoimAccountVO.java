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
public class MoimAccountVO {
    private String moimAccountId;
    private String userId;
    private String kbAccountId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String accountName;
}
