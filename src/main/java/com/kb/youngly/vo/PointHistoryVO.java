package com.kb.youngly.vo;

import com.kb.youngly.enums.PointType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryVO {
    private Long pointLogId;
    private String userId;
    private Long itemId;
    private PointType pointType;
    private Integer amount;
    private String content;
    private String idempotencyKey;
    private LocalDateTime createdAt;
}
