package com.kb.youngly.service;

import com.kb.youngly.client.FssMarketApiClient;
import com.kb.youngly.dto.market.FssMarketItem;
import com.kb.youngly.dto.market.MarketSnapshotIngestResult;
import com.kb.youngly.mapper.MarketDailySnapshotMapper;
import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MarketSnapshotIngestServiceTest {

    @Test
    @DisplayName("오후동향 PDF만 다운로드/파싱/저장하고 HWP는 제외한다")
    void ingest_filtersAfternoonPdfAndSkipsHwp() {
        FssMarketApiClient apiClient = mock(FssMarketApiClient.class);
        FssMarketPdfDownloadService downloadService = mock(FssMarketPdfDownloadService.class);
        PdfParseService parseService = mock(PdfParseService.class);
        MarketDailySnapshotMapper mapper = mock(MarketDailySnapshotMapper.class);

        FssMarketItem item = new FssMarketItem();
        item.setSubject("금융시장동향");
        item.setRegDate("2026-08-04");
        item.setAtchfileNm("20260804_오후동향.pdf|주간시장지표(홈페이지 게시)_F.hwp");
        item.setAtchfileUrl("https://example.com/afternoon.pdf|https://example.com/weekly.hwp");

        when(apiClient.fetchMarketItems(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(item));
        when(downloadService.download("https://example.com/afternoon.pdf")).thenReturn(new byte[]{1, 2, 3});
        when(parseService.extractText(new byte[]{1, 2, 3})).thenReturn("KOSPI 3000\nUSD/KRW 1380");
        when(mapper.upsertMarketDailySnapshot(any(MarketDailySnapshotVO.class))).thenReturn(1);

        MarketSnapshotIngestService service = new MarketSnapshotIngestService(
                apiClient, downloadService, parseService, mapper);

        MarketSnapshotIngestResult result = service.ingest(LocalDate.of(2026, 7, 21), LocalDate.of(2026, 8, 4));

        assertEquals(1, result.totalItems());
        assertEquals(1, result.targetPdfCount());
        assertEquals(1, result.savedCount());
        assertEquals(1, result.skippedCount());
        assertEquals(0, result.failedCount());

        verify(mapper, times(1)).upsertMarketDailySnapshot(any(MarketDailySnapshotVO.class));
        assertTrue(result.savedCount() == 1);
    }

    @Test
    @DisplayName("ingestRecentTwoWeeks는 전날부터 14일 전까지 총 14일 범위를 재수집한다")
    void ingestRecentTwoWeeks_reingestsYesterdayToFourteenDaysAgo() {
        FssMarketApiClient apiClient = mock(FssMarketApiClient.class);
        FssMarketPdfDownloadService downloadService = mock(FssMarketPdfDownloadService.class);
        PdfParseService parseService = mock(PdfParseService.class);
        MarketDailySnapshotMapper mapper = mock(MarketDailySnapshotMapper.class);

        when(apiClient.fetchMarketItems(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());

        MarketSnapshotIngestService service = new MarketSnapshotIngestService(
                apiClient, downloadService, parseService, mapper);

        service.ingestRecentTwoWeeks();

        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        verify(apiClient).fetchMarketItems(eq(today.minusDays(14)), eq(today.minusDays(1)));
    }
}
