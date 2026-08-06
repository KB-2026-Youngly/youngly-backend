package com.kb.youngly.dto.market;

public record MarketSnapshotIngestResult(
    int totalItems,
    int targetPdfCount,
    int savedCount,
    int skippedCount,
    int failedCount
) {}
