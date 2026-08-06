package com.kb.youngly.dto.market;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MarketSnapshotIngestRequest {

    private LocalDate from;
    private LocalDate to;
}
