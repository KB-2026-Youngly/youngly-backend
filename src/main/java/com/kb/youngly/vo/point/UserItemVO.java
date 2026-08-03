package com.kb.youngly.vo.point;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserItemVO {
    private Long userItemId;
    private Long itemId;
    private String userId;
    private Boolean isEquipped;
    private LocalDateTime createdAt;
}
