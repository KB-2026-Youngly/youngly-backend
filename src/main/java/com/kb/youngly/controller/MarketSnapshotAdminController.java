package com.kb.youngly.controller;

import com.kb.youngly.dto.market.MarketSnapshotIngestRequest;
import com.kb.youngly.dto.market.MarketSnapshotIngestResult;
import com.kb.youngly.mapper.MarketDailySnapshotMapper;
import com.kb.youngly.service.MarketSnapshotIngestService;
import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/market-snapshots")
public class MarketSnapshotAdminController {

    private final MarketSnapshotIngestService ingestService;
    private final MarketDailySnapshotMapper snapshotMapper;

    public MarketSnapshotAdminController(MarketSnapshotIngestService ingestService,
                                         MarketDailySnapshotMapper snapshotMapper) {
        this.ingestService = ingestService;
        this.snapshotMapper = snapshotMapper;
    }

    @PostMapping("/ingest/recent-two-weeks")
    public ResponseEntity<MarketSnapshotIngestResult> ingestRecentTwoWeeks() {
        return ResponseEntity.ok(ingestService.ingestRecentTwoWeeks());
    }

    @PostMapping("/ingest")
    public ResponseEntity<MarketSnapshotIngestResult> ingest(@RequestBody MarketSnapshotIngestRequest request) {
        Assert.notNull(request, "request must not be null");
        Assert.notNull(request.getFrom(), "from must not be null");
        Assert.notNull(request.getTo(), "to must not be null");
        return ResponseEntity.ok(ingestService.ingest(request.getFrom(), request.getTo()));
    }

    @GetMapping
    public ResponseEntity<List<MarketDailySnapshotVO>> findRecentSnapshots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(snapshotMapper.findRecentSnapshots(from, to, limit));
    }
}
