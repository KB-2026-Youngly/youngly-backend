package com.kb.youngly.service;

import com.kb.youngly.dto.market.MarketSnapshotIngestResult;
import com.kb.youngly.mapper.MarketDailySnapshotMapper;
import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = com.kb.youngly.config.RootConfig.class)
class MarketSnapshotIngestIntegrationTest {

    @Autowired
    private MarketSnapshotIngestService ingestService;

    @Autowired
    private MarketDailySnapshotMapper mapper;

    @AfterEach
    void cleanup() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(14);
        List<MarketDailySnapshotVO> recent = mapper.findRecentSnapshots(start, end, 200);
        for (MarketDailySnapshotVO snapshot : recent) {
            if (snapshot.getPdfFileName() != null) {
                mapper.deleteByPdfFileName(snapshot.getPdfFileName());
            }
        }
    }

    @Test
    @DisplayName("최근 2주치 금감원 금융시장동향 PDF를 실제 수집하고 DB에 저장한다")
    void ingestRecentTwoWeeks_realPipeline_savesRows() {
        MarketSnapshotIngestResult result = ingestService.ingestRecentTwoWeeks();

        assertTrue(result.targetPdfCount() > 0);
        assertTrue(result.savedCount() > 0);

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(14);
        List<MarketDailySnapshotVO> recent = mapper.findRecentSnapshots(start, end, 20);

        assertFalse(recent.isEmpty());
        assertNotNull(recent.get(0).getRawText());
        assertTrue(!recent.get(0).getRawText().isBlank());
    }

    @Test
    @DisplayName("같은 기간을 두 번 실행해도 pdf_file_name 기준 중복 row가 늘지 않는다")
    void ingestRecentTwoWeeks_twice_keepsRowCountStable() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(14);

        ingestService.ingestRecentTwoWeeks();
        int firstRowCount = mapper.findRecentSnapshots(start, end, 200).size();

        ingestService.ingestRecentTwoWeeks();
        int secondRowCount = mapper.findRecentSnapshots(start, end, 200).size();

        assertTrue(firstRowCount > 0);
        assertTrue(secondRowCount > 0);
        assertTrue(firstRowCount == secondRowCount);
    }
}
