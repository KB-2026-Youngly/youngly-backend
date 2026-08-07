package com.kb.youngly.dto.round;

import com.kb.youngly.enums.RoundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundResponse {

    private Long roundId;

    private Integer roundNo;

    private LocalDate startDate;

    private LocalDate endDate;

    private RoundStatus roundStatus;
}