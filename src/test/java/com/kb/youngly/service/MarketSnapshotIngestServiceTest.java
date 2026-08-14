package com.kb.youngly.service;

import com.kb.youngly.client.FssMarketApiClient;
import com.kb.youngly.dto.market.FssMarketItem;
import com.kb.youngly.dto.market.MarketSnapshotIngestResult;
import com.kb.youngly.mapper.MarketDailySnapshotMapper;
import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MarketSnapshotIngestServiceTest {

    private FssMarketApiClient apiClient;
    private FssMarketPdfDownloadService downloadService;
    private PdfParseService parseService;
    private MarketDailySnapshotMapper mapper;
    private MarketSnapshotIngestService service;

    @BeforeEach
    void setUp() {
        apiClient = mock(FssMarketApiClient.class);
        downloadService = mock(FssMarketPdfDownloadService.class);
        parseService = mock(PdfParseService.class);
        mapper = mock(MarketDailySnapshotMapper.class);
        service = new MarketSnapshotIngestService(apiClient, downloadService, parseService, mapper);

        when(mapper.findRecentSnapshots(any(LocalDate.class), any(LocalDate.class), anyInt()))
                .thenReturn(List.of());
    }

    @Test
    void ingestRecentTwoWeeksFetchesMarketItemsWithSingleRangeCall() {
        when(apiClient.fetchMarketItems(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());

        service.ingestRecentTwoWeeks();

        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        verify(apiClient, times(1)).fetchMarketItems(today.minusDays(14), today.minusDays(1));
    }

    @Test
    void ingestCallsFetchMarketItemsOnceEvenWhenMultipleItemsExist() {
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 14);
        when(apiClient.fetchMarketItems(startDate, endDate)).thenReturn(List.of(
                item("2026-08-13", "20260813_오후동향.pdf", "https://example.com/13.pdf"),
                item("2026-08-14", "20260814_오후동향.pdf", "https://example.com/14.pdf")
        ));
        when(downloadService.download(any())).thenReturn(new byte[]{1});
        when(parseService.extractText(any())).thenReturn("시장동향 원문");
        when(mapper.upsertMarketDailySnapshot(any(MarketDailySnapshotVO.class))).thenReturn(1);

        MarketSnapshotIngestResult result = service.ingest(startDate, endDate);

        assertEquals(2, result.totalItems());
        assertEquals(2, result.savedCount());
        verify(apiClient, times(1)).fetchMarketItems(startDate, endDate);
        verify(downloadService, times(2)).download(any());
        verify(parseService, times(2)).extractText(any());
        verify(mapper, times(2)).upsertMarketDailySnapshot(any(MarketDailySnapshotVO.class));
    }

    @Test
    void skipsPdfDownloadParseAndUpsertWhenExistingRawTextIsReusable() {
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 14);
        LocalDate existingDate = LocalDate.of(2026, 8, 13);
        when(mapper.findRecentSnapshots(startDate, endDate, 100))
                .thenReturn(List.of(snapshot(existingDate, "기존 원문", null)));
        when(apiClient.fetchMarketItems(startDate, endDate)).thenReturn(List.of(
                item("2026-08-13", "20260813_오후동향.pdf", "https://example.com/13.pdf")
        ));

        MarketSnapshotIngestResult result = service.ingest(startDate, endDate);

        assertEquals(1, result.totalItems());
        assertEquals(0, result.targetPdfCount());
        assertEquals(0, result.savedCount());
        assertEquals(1, result.skippedCount());
        verify(apiClient, times(1)).fetchMarketItems(startDate, endDate);
        verifyNoInteractions(downloadService, parseService);
        verify(mapper, never()).upsertMarketDailySnapshot(any(MarketDailySnapshotVO.class));
    }

    @Test
    void skipsPdfDownloadParseAndUpsertWhenExistingSummaryTextIsReusable() {
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 14);
        LocalDate existingDate = LocalDate.of(2026, 8, 13);
        when(mapper.findRecentSnapshots(startDate, endDate, 100))
                .thenReturn(List.of(snapshot(existingDate, null, "기존 요약")));
        when(apiClient.fetchMarketItems(startDate, endDate)).thenReturn(List.of(
                item("2026-08-13", "20260813_오후동향.pdf", "https://example.com/13.pdf")
        ));

        service.ingest(startDate, endDate);

        verify(apiClient, times(1)).fetchMarketItems(startDate, endDate);
        verifyNoInteractions(downloadService, parseService);
        verify(mapper, never()).upsertMarketDailySnapshot(any(MarketDailySnapshotVO.class));
    }

    @Test
    void processesMissingDateWithPdfDownloadParseAndUpsert() {
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 14);
        when(apiClient.fetchMarketItems(startDate, endDate)).thenReturn(List.of(
                item("2026-08-13", "20260813_오후동향.pdf", "https://example.com/13.pdf")
        ));
        when(downloadService.download("https://example.com/13.pdf")).thenReturn(new byte[]{1, 2, 3});
        when(parseService.extractText(new byte[]{1, 2, 3})).thenReturn("KOSPI 3000\nUSD/KRW 1380");
        when(mapper.upsertMarketDailySnapshot(any(MarketDailySnapshotVO.class))).thenReturn(1);

        MarketSnapshotIngestResult result = service.ingest(startDate, endDate);

        assertEquals(1, result.totalItems());
        assertEquals(1, result.targetPdfCount());
        assertEquals(1, result.savedCount());
        assertEquals(0, result.failedCount());
        verify(downloadService).download("https://example.com/13.pdf");
        verify(parseService).extractText(new byte[]{1, 2, 3});
        verify(mapper).upsertMarketDailySnapshot(any(MarketDailySnapshotVO.class));
    }

    @Test
    void mixedPdfAndHwpAttachmentsProcessesOnlyAfternoonPdf() {
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 14);
        when(apiClient.fetchMarketItems(startDate, endDate)).thenReturn(List.of(
                item("2026-08-13",
                        "20260813_오후동향.pdf|주간시장지표.hwp",
                        "https://example.com/afternoon.pdf|https://example.com/weekly.hwp")
        ));
        when(downloadService.download("https://example.com/afternoon.pdf")).thenReturn(new byte[]{1});
        when(parseService.extractText(new byte[]{1})).thenReturn("시장동향 원문");
        when(mapper.upsertMarketDailySnapshot(any(MarketDailySnapshotVO.class))).thenReturn(1);

        MarketSnapshotIngestResult result = service.ingest(startDate, endDate);

        assertEquals(1, result.totalItems());
        assertEquals(1, result.targetPdfCount());
        assertEquals(1, result.savedCount());
        assertEquals(1, result.skippedCount());
        verify(apiClient, times(1)).fetchMarketItems(startDate, endDate);
        verify(downloadService, times(1)).download("https://example.com/afternoon.pdf");
        verify(downloadService, never()).download("https://example.com/weekly.hwp");
        verify(parseService, times(1)).extractText(any());
        verify(mapper, times(1)).upsertMarketDailySnapshot(any(MarketDailySnapshotVO.class));
    }

    private static FssMarketItem item(String regDate, String fileNames, String urls) {
        FssMarketItem item = new FssMarketItem();
        item.setSubject("금융시장동향");
        item.setRegDate(regDate);
        item.setAtchfileNm(fileNames);
        item.setAtchfileUrl(urls);
        return item;
    }

    private static MarketDailySnapshotVO snapshot(LocalDate marketDate,
                                                  String rawText,
                                                  String summaryText) {
        return MarketDailySnapshotVO.builder()
                .marketDate(marketDate)
                .rawText(rawText)
                .summaryText(summaryText)
                .build();
    }
}
