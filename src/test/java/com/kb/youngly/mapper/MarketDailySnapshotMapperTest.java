package com.kb.youngly.mapper;

import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "file:src/main/webapp/WEB-INF/spring/root-context.xml"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MarketDailySnapshotMapperTest {

    @Autowired
    private MarketDailySnapshotMapper mapper;

    private final String f1 = "260604 오후동향_F.pdf";
    private final String f2 = "260605 오후동향_F.pdf";
    private final String dateSubject = "일일 금융시장 동향[8.4일]";

    @AfterEach
    void tearDown() {
        mapper.deleteByPdfFileName(f1);
        mapper.deleteByPdfFileName(f2);
        mapper.deleteByMarketDateAndSourceSubject(LocalDate.of(2026, 8, 4), dateSubject);
    }

    @Test
    @DisplayName("upsert 후 pdfFileName으로 조회된다")
    void upsertAndFindByPdfFileName() {
        MarketDailySnapshotVO vo = createSnapshot(
                "2026-08-04",
                "일일 금융시장 동향[8.4일]",
                f1,
                "https://example.com/260804.pdf",
                "KOSPI 3000 / USDKRW 1380",
                "KOSPI 상승"
        );

        int affected = mapper.upsertMarketDailySnapshot(vo);
        assertEquals(1, affected);

        MarketDailySnapshotVO found = mapper.findByPdfFileName(f1);
        assertNotNull(found);
        assertEquals(LocalDate.of(2026, 8, 4), found.getMarketDate());
        assertEquals("일일 금융시장 동향[8.4일]", found.getSourceSubject());
        assertEquals(f1, found.getPdfFileName());
        assertEquals("https://example.com/260804.pdf", found.getPdfUrl());
        assertTrue(found.getRawText().contains("KOSPI"));
        assertTrue(found.getSummaryText().contains("상승"));
        assertEquals(new BigDecimal("3000.12"), found.getKospi());
        assertEquals(new BigDecimal("800.34"), found.getKosdaq());
        assertEquals(new BigDecimal("20000.56"), found.getNasdaq());
        assertEquals(new BigDecimal("1380.25"), found.getUsdkrw());
        assertEquals(new BigDecimal("3.4567"), found.getTreasury3y());
    }

    @Test
    @DisplayName("같은 pdfFileName으로 upsert하면 기존 레코드가 갱신된다")
    void upsertDuplicatePdfFileName_updatesRow() {
        MarketDailySnapshotVO first = createSnapshot(
                "2026-08-04",
                "일일 금융시장 동향[8.4일]",
                f1,
                "https://example.com/260804-v1.pdf",
                "RAW-1",
                "SUMMARY-1"
        );
        mapper.upsertMarketDailySnapshot(first);

        MarketDailySnapshotVO second = createSnapshot(
                "2026-08-04",
                "일일 금융시장 동향[8.4일]",
                f1,
                "https://example.com/260804-v2.pdf",
                "RAW-2",
                "SUMMARY-2"
        );
        mapper.upsertMarketDailySnapshot(second);

        MarketDailySnapshotVO found = mapper.findByPdfFileName(f1);
        assertNotNull(found);
        assertEquals("https://example.com/260804-v2.pdf", found.getPdfUrl());
        assertEquals("RAW-2", found.getRawText());
        assertEquals("SUMMARY-2", found.getSummaryText());
    }

    @Test
    @DisplayName("같은 marketDate/sourceSubject로 upsert하면 기존 레코드가 갱신된다")
    void upsertDuplicateMarketDateAndSourceSubject_updatesRow() {
        MarketDailySnapshotVO first = createSnapshot(
                "2026-08-04",
                dateSubject,
                f1,
                "https://example.com/260804-v1.pdf",
                "RAW-1",
                "SUMMARY-1"
        );
        mapper.upsertMarketDailySnapshot(first);

        MarketDailySnapshotVO second = createSnapshot(
                "2026-08-04",
                dateSubject,
                f2,
                "https://example.com/260804-v2.pdf",
                "RAW-2",
                "SUMMARY-2"
        );
        mapper.upsertMarketDailySnapshot(second);

        MarketDailySnapshotVO found = mapper.findByMarketDateAndSourceSubject(
                LocalDate.of(2026, 8, 4),
                dateSubject
        );

        assertNotNull(found);
        assertEquals(f2, found.getPdfFileName());
        assertEquals("https://example.com/260804-v2.pdf", found.getPdfUrl());
        assertEquals("RAW-2", found.getRawText());
        assertEquals("SUMMARY-2", found.getSummaryText());
    }

    @Test
    @DisplayName("최근 기간 조회가 정상 동작한다")
    void findRecentSnapshots() {
        mapper.upsertMarketDailySnapshot(createSnapshot(
                "2026-08-04",
                "일일 금융시장 동향[8.4일]",
                f1,
                "https://example.com/260804.pdf",
                "RAW-1",
                "SUMMARY-1"
        ));
        mapper.upsertMarketDailySnapshot(createSnapshot(
                "2026-08-05",
                "일일 금융시장 동향[8.5일]",
                f2,
                "https://example.com/260805.pdf",
                "RAW-2",
                "SUMMARY-2"
        ));

        List<MarketDailySnapshotVO> recent = mapper.findRecentSnapshots(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                10
        );

        assertFalse(recent.isEmpty());
        assertTrue(recent.stream().anyMatch(v -> f1.equals(v.getPdfFileName())));
        assertTrue(recent.stream().anyMatch(v -> f2.equals(v.getPdfFileName())));
    }

    private MarketDailySnapshotVO createSnapshot(
            String marketDate,
            String subject,
            String fileName,
            String url,
            String rawText,
            String summaryText
    ) {
        return MarketDailySnapshotVO.builder()
                .marketDate(LocalDate.parse(marketDate))
                .sourceSubject(subject)
                .pdfFileName(fileName)
                .pdfUrl(url)
                .rawText(rawText)
                .summaryText(summaryText)
                .kospi(new BigDecimal("3000.12"))
                .kosdaq(new BigDecimal("800.34"))
                .nasdaq(new BigDecimal("20000.56"))
                .usdkrw(new BigDecimal("1380.25"))
                .treasury3y(new BigDecimal("3.4567"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
