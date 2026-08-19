package com.kb.youngly.dto.demo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DemoTimeAdvanceRequest {
    private Integer days;
    private Boolean includeMarketScheduler = Boolean.TRUE;
}
