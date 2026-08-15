package com.kb.youngly.service;

import com.kb.youngly.client.OpenAiClient;
import com.kb.youngly.dto.market.MarketSummaryResponse;
import com.kb.youngly.mapper.MarketDailySnapshotMapper;
import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MarketSnapshotSummaryServiceTest {

    private MarketDailySnapshotMapper mapper;
    private OpenAiClient openAiClient;
    private ObjectProvider<OpenAiClient> openAiClientProvider;
    private MarketSnapshotSummaryService service;

    @BeforeEach
    void setUp() {
        mapper = mock(MarketDailySnapshotMapper.class);
        openAiClient = mock(OpenAiClient.class);
        openAiClientProvider = mock(ObjectProvider.class);
        when(openAiClientProvider.getObject()).thenReturn(openAiClient);
        service = new MarketSnapshotSummaryService(mapper, openAiClientProvider);
    }

    @Test
    void skipsWhenDailySummaryAlreadyExists() {
        MarketDailySnapshotVO snapshot = snapshot(1L, "RAW");
        snapshot.setMarketHeadlineText("이미 생성된 요약");
        snapshot.setMarketDetailText("이미 생성된 상세 설명입니다.");
        when(mapper.findSnapshotsByMarketDate(LocalDate.of(2026, 8, 13))).thenReturn(List.of(snapshot));

        service.generateDailySummaryIfAbsent(LocalDate.of(2026, 8, 13));

        verifyNoInteractions(openAiClient);
        verify(mapper, never()).updateMarketSummary(anyLong(), anyString(), anyString());
    }

    @Test
    void generatesAndStoresDailySummaryWhenAbsent() {
        when(mapper.findSnapshotsByMarketDate(LocalDate.of(2026, 8, 13)))
                .thenReturn(List.of(snapshot(10L, "시장 원문입니다.")));
        when(openAiClient.generateMarketSummary(any()))
                .thenReturn(new MarketSummaryResponse("금리 흐름 안정", "시장 원문을 바탕으로 작성한 상세 설명입니다."));

        service.generateDailySummaryIfAbsent(LocalDate.of(2026, 8, 13));

        verify(openAiClient, times(1)).generateMarketSummary(any());
        verify(mapper).updateMarketSummary(
                eq(10L),
                eq("금리 흐름 안정"),
                eq("시장 원문을 바탕으로 작성한 상세 설명입니다.")
        );
    }

    @Test
    void generatesLatestRecentSummaryIntoLatestValidMarketDateRow() {
        LocalDate startDate = LocalDate.of(2026, 7, 31);
        LocalDate endDate = LocalDate.of(2026, 8, 13);
        MarketDailySnapshotVO latest = snapshot(13L, "8월 13일 시장 원문입니다.");
        latest.setMarketDate(LocalDate.of(2026, 8, 13));
        MarketDailySnapshotVO previous = snapshot(12L, "8월 12일 시장 원문입니다.");
        previous.setMarketDate(LocalDate.of(2026, 8, 12));
        when(mapper.findRecentSnapshots(startDate, endDate, 50))
                .thenReturn(List.of(latest, previous));
        when(openAiClient.generateMarketSummary(any()))
                .thenReturn(new MarketSummaryResponse("최근 시장 흐름 정리", "최근 14일 원문을 바탕으로 작성한 상세 설명입니다."));
        when(mapper.updateMarketSummaryIfAbsent(anyLong(), anyString(), anyString())).thenReturn(1);

        service.generateLatestRecentSummaryIfAbsent(startDate, endDate);

        verify(openAiClient, times(1)).generateMarketSummary(any());
        verify(mapper).updateMarketSummaryIfAbsent(
                eq(13L),
                eq("최근 시장 흐름 정리"),
                eq("최근 14일 원문을 바탕으로 작성한 상세 설명입니다.")
        );
    }

    @Test
    void skipsLatestRecentSummaryWhenLatestRowAlreadyHasSummary() {
        LocalDate startDate = LocalDate.of(2026, 7, 31);
        LocalDate endDate = LocalDate.of(2026, 8, 13);
        MarketDailySnapshotVO latest = snapshot(13L, "8월 13일 시장 원문입니다.");
        latest.setMarketDate(LocalDate.of(2026, 8, 13));
        latest.setMarketHeadlineText("이미 생성된 요약");
        latest.setMarketDetailText("이미 생성된 상세 설명입니다.");
        when(mapper.findRecentSnapshots(startDate, endDate, 50)).thenReturn(List.of(latest));

        service.generateLatestRecentSummaryIfAbsent(startDate, endDate);

        verifyNoInteractions(openAiClient);
        verify(mapper, never()).updateMarketSummaryIfAbsent(anyLong(), anyString(), anyString());
    }

    private static MarketDailySnapshotVO snapshot(Long id, String rawText) {
        MarketDailySnapshotVO snapshot = new MarketDailySnapshotVO();
        snapshot.setId(id);
        snapshot.setSourceSubject("일일 금융시장 동향");
        snapshot.setRawText(rawText);
        return snapshot;
    }
}
