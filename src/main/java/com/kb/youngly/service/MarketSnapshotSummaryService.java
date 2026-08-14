package com.kb.youngly.service;

import com.kb.youngly.client.OpenAiClient;
import com.kb.youngly.dto.market.MarketSummaryResponse;
import com.kb.youngly.dto.recommendation.RecommendationPrompt;
import com.kb.youngly.mapper.MarketDailySnapshotMapper;
import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class MarketSnapshotSummaryService {

    private static final Logger log = LogManager.getLogger(MarketSnapshotSummaryService.class);

    private static final int RAW_CONTEXT_MAX_LENGTH = 6_000;

    private static final String SYSTEM_MESSAGE = """
            당신은 Youngly 서비스의 공용 금융시장 동향 요약 작성자입니다.
            입력 원문에 있는 사실만 사용하고, 전망·투자 권유·추천을 만들지 마세요.

            [작성 규칙]
            - headline: 프론트 카드에 표시할 개조식 요약입니다. 2~4개 항목을 줄바꿈 또는 세미콜론으로 짧게 연결하세요.
            - detail: 자세히보기 영역에 표시할 설명입니다. 원문에 있는 흐름만 2~4문장으로 정리하세요.
            - 특정 종목이나 상품 매수·매도·가입을 권유하지 마세요.
            - JSON 외의 설명, Markdown, 코드 블록을 출력하지 마세요.

            [출력 JSON 형태]
            {
              "headline": "개조식 요약",
              "detail": "자세히보기 상세 설명"
            }
            """;

    private final MarketDailySnapshotMapper marketDailySnapshotMapper;
    private final ObjectProvider<OpenAiClient> openAiClientProvider;

    public MarketSnapshotSummaryService(MarketDailySnapshotMapper marketDailySnapshotMapper,
                                        ObjectProvider<OpenAiClient> openAiClientProvider) {
        this.marketDailySnapshotMapper = marketDailySnapshotMapper;
        this.openAiClientProvider = openAiClientProvider;
    }

    public void generateDailySummaryIfAbsent(LocalDate marketDate) {
        if (marketDate == null) {
            return;
        }

        List<MarketDailySnapshotVO> snapshots = marketDailySnapshotMapper.findSnapshotsByMarketDate(marketDate);
        if (snapshots.isEmpty()) {
            return;
        }

        boolean alreadyGenerated = snapshots.stream()
                .anyMatch(snapshot -> StringUtils.hasText(snapshot.getMarketHeadlineText())
                        && StringUtils.hasText(snapshot.getMarketDetailText()));
        if (alreadyGenerated) {
            log.info("[MARKET_SUMMARY_SKIPPED] 이미 생성된 시장요약이 있습니다. marketDate={}", marketDate);
            return;
        }

        MarketDailySnapshotVO target = snapshots.get(0);
        String context = buildContext(snapshots);
        if (!StringUtils.hasText(context)) {
            log.warn("[MARKET_SUMMARY_SKIPPED] 시장요약 생성용 원문이 비어 있습니다. marketDate={}", marketDate);
            return;
        }

        MarketSummaryResponse response = openAiClientProvider.getObject()
                .generateMarketSummary(new RecommendationPrompt(
                        SYSTEM_MESSAGE,
                        "[시장동향 날짜]\n" + marketDate + "\n\n[시장동향 원문]\n" + context
                ));

        marketDailySnapshotMapper.updateMarketSummary(
                target.getId(),
                response.headline(),
                response.detail()
        );
        log.info("[MARKET_SUMMARY_GENERATED] marketDate={}, snapshotId={}", marketDate, target.getId());
    }

    public synchronized void generateLatestRecentSummaryIfAbsent(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return;
        }

        List<MarketDailySnapshotVO> snapshots = marketDailySnapshotMapper.findRecentSnapshots(startDate, endDate, 50);
        if (snapshots.isEmpty()) {
            log.warn("[MARKET_SUMMARY_SKIPPED] 최근 시장동향 원문이 없습니다. startDate={}, endDate={}", startDate, endDate);
            return;
        }

        MarketDailySnapshotVO latest = snapshots.stream()
                .filter(snapshot -> snapshot.getMarketDate() != null)
                .findFirst()
                .orElse(null);
        if (latest == null) {
            log.warn("[MARKET_SUMMARY_SKIPPED] 유효한 marketDate가 없습니다. startDate={}, endDate={}", startDate, endDate);
            return;
        }

        if (StringUtils.hasText(latest.getMarketHeadlineText())
                && StringUtils.hasText(latest.getMarketDetailText())) {
            log.info("[MARKET_SUMMARY_SKIPPED] 최신 유효 시장요약이 이미 있습니다. marketDate={}, snapshotId={}",
                    latest.getMarketDate(), latest.getId());
            return;
        }

        String context = buildContext(snapshots);
        if (!StringUtils.hasText(context)) {
            log.warn("[MARKET_SUMMARY_SKIPPED] 시장요약 생성용 최근 14일 원문이 비어 있습니다. marketDate={}",
                    latest.getMarketDate());
            return;
        }

        MarketSummaryResponse response = openAiClientProvider.getObject()
                .generateMarketSummary(new RecommendationPrompt(
                        SYSTEM_MESSAGE,
                        "[시장동향 범위]\n" + startDate + " ~ " + endDate
                                + "\n\n[최신 유효 시장동향 날짜]\n" + latest.getMarketDate()
                                + "\n\n[최근 14일 시장동향 원문]\n" + context
                ));

        int updated = marketDailySnapshotMapper.updateMarketSummaryIfAbsent(
                latest.getId(),
                response.headline(),
                response.detail()
        );
        if (updated == 0) {
            log.info("[MARKET_SUMMARY_SKIPPED] 다른 실행이 먼저 시장요약을 저장했습니다. marketDate={}, snapshotId={}",
                    latest.getMarketDate(), latest.getId());
            return;
        }

        log.info("[MARKET_SUMMARY_GENERATED] marketDate={}, snapshotId={}", latest.getMarketDate(), latest.getId());
    }

    private String buildContext(List<MarketDailySnapshotVO> snapshots) {
        StringBuilder builder = new StringBuilder();
        for (MarketDailySnapshotVO snapshot : snapshots) {
            String text = StringUtils.hasText(snapshot.getSummaryText())
                    ? snapshot.getSummaryText()
                    : snapshot.getRawText();
            if (!StringUtils.hasText(text)) {
                continue;
            }

            String entry = "[" + snapshot.getSourceSubject() + "] "
                    + text.replaceAll("\\s+", " ").trim();
            if (builder.length() > 0) {
                entry = "\n\n" + entry;
            }

            int remaining = RAW_CONTEXT_MAX_LENGTH - builder.length();
            if (remaining <= 0) {
                break;
            }
            builder.append(entry, 0, Math.min(entry.length(), remaining));
        }
        return builder.toString();
    }
}
