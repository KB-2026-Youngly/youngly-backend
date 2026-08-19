package com.kb.youngly.dto.demo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class DemoTimeApplyRequest {
    private LocalDate date;
    private Boolean includeMarketScheduler = Boolean.TRUE;
}
