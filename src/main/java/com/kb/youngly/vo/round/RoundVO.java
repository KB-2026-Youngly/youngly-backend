package com.kb.youngly.vo.round;

import com.kb.youngly.enums.RoundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundVO {
    private Long roundId;
    private String groupId;
    private Integer roundNo;
    private LocalDate startDate;
    private LocalDate endDate;
    private RoundStatus roundStatus;
    private LocalDateTime createdAt;
}
