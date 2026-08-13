package com.kb.youngly.service;

import com.kb.youngly.client.FssMarketApiClient;
import com.kb.youngly.dto.market.FssMarketItem;
import com.kb.youngly.dto.market.MarketSnapshotIngestResult;
import com.kb.youngly.exception.FssMarketPdfDownloadException;
import com.kb.youngly.exception.PdfParseException;
import com.kb.youngly.mapper.MarketDailySnapshotMapper;
import com.kb.youngly.vo.market.MarketDailySnapshotVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarketSnapshotIngestService {

    private static final Logger log = LogManager.getLogger(MarketSnapshotIngestService.class);
    private static final int SUMMARY_MAX_LENGTH = 4_000;
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final ZoneId DEFAULT_MARKET_ZONE = ZoneId.of("Asia/Seoul");

    private final FssMarketApiClient fssMarketApiClient;
    private final FssMarketPdfDownloadService pdfDownloadService;
    private final PdfParseService pdfParseService;
    private final MarketDailySnapshotMapper marketDailySnapshotMapper;

    public MarketSnapshotIngestService(FssMarketApiClient fssMarketApiClient,
                                       FssMarketPdfDownloadService pdfDownloadService,
                                       PdfParseService pdfParseService,
                                       MarketDailySnapshotMapper marketDailySnapshotMapper) {
        this.fssMarketApiClient = fssMarketApiClient;
        this.pdfDownloadService = pdfDownloadService;
        this.pdfParseService = pdfParseService;
        this.marketDailySnapshotMapper = marketDailySnapshotMapper;
    }

    public MarketSnapshotIngestResult ingestRecentTwoWeeks() {
        LocalDate today = LocalDate.now(DEFAULT_MARKET_ZONE);
        return ingest(today.minusDays(14), today.minusDays(1));
    }

    public MarketSnapshotIngestResult ingest(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");

        List<FssMarketItem> items = fssMarketApiClient.fetchMarketItems(startDate, endDate);
        int targetPdfCount = 0;
        int savedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (FssMarketItem item : items) {
            List<Attachment> attachments = resolveAttachments(item);
            if (attachments.isEmpty()) {
                skippedCount++;
                continue;
            }

            for (Attachment attachment : attachments) {
                if (!isTargetAfternoonPdf(attachment.getFileName())) {
                    skippedCount++;
                    continue;
                }

                targetPdfCount++;
                try {
                    Optional<LocalDate> marketDate = resolveMarketDate(item);
                    if (marketDate.isEmpty()) {
                        log.warn("[WARN] 시장동향 항목의 날짜 파싱 실패. subject={}, regDate={}, fileName={}",
                                item.getSubject(), item.getRegDate(), attachment.getFileName());
                        failedCount++;
                        continue;
                    }

                    byte[] pdfBytes = pdfDownloadService.download(attachment.getUrl());
                    String rawText = pdfParseService.extractText(pdfBytes);
                    if (!StringUtils.hasText(rawText)) {
                        log.warn("[WARN] PDF 텍스트 추출 결과가 비어 있습니다. fileName={}, pdfUrl={}",
                                attachment.getFileName(), attachment.getUrl());
                    }

                    MarketDailySnapshotVO snapshot = MarketDailySnapshotVO.builder()
                            .marketDate(marketDate.get())
                            .sourceSubject(item.getSubject())
                            .pdfFileName(attachment.getFileName())
                            .pdfUrl(attachment.getUrl())
                            .rawText(rawText)
                            .summaryText(toPromptSummary(rawText))
                            .build();

                    int affectedRows = marketDailySnapshotMapper.upsertMarketDailySnapshot(snapshot);
                    if (affectedRows < 1) {
                        throw new IllegalStateException("market_daily_snapshot 저장 결과가 0건입니다.");
                    }
                    savedCount++;
                } catch (FssMarketPdfDownloadException e) {
                    failedCount++;
                    log.error("[ERROR] 시장동향 PDF 다운로드 단계 실패. subject={}, fileName={}, pdfUrl={}",
                            item.getSubject(), attachment.getFileName(), attachment.getUrl(), e);
                } catch (PdfParseException e) {
                    failedCount++;
                    log.error("[ERROR] 시장동향 PDF 파싱 단계 실패. subject={}, fileName={}, pdfUrl={}",
                            item.getSubject(), attachment.getFileName(), attachment.getUrl(), e);
                } catch (RuntimeException e) {
                    failedCount++;
                    log.error("[ERROR] 시장동향 DB 저장 단계 실패. subject={}, fileName={}, pdfUrl={}",
                            item.getSubject(), attachment.getFileName(), attachment.getUrl(), e);
                }
            }
        }

        log.info("[INFO] 시장동향 스냅샷 수집 완료. totalItems={}, targetPdfCount={}, savedCount={}, skippedCount={}, failedCount={}",
                items.size(), targetPdfCount, savedCount, skippedCount, failedCount);
        return new MarketSnapshotIngestResult(items.size(), targetPdfCount, savedCount, skippedCount, failedCount);
    }

    private List<Attachment> resolveAttachments(FssMarketItem item) {
        if (item == null || !StringUtils.hasText(item.getAtchfileNm()) || !StringUtils.hasText(item.getAtchfileUrl())) {
            return List.of();
        }

        String[] fileNames = item.getAtchfileNm().split("\\|");
        String[] urls = item.getAtchfileUrl().split("\\|");
        int count = Math.min(fileNames.length, urls.length);

        List<Attachment> attachments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String fileName = fileNames[i].trim();
            String url = urls[i].trim();
            if (StringUtils.hasText(fileName) && StringUtils.hasText(url)) {
                attachments.add(new Attachment(fileName, url));
            }
        }
        return attachments;
    }

    private boolean isTargetAfternoonPdf(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return fileName.contains("오후동향") && lower.endsWith(".pdf");
    }

    private Optional<LocalDate> resolveMarketDate(FssMarketItem item) {
        if (item == null || !StringUtils.hasText(item.getRegDate())) {
            return Optional.empty();
        }

        String regDate = item.getRegDate().trim();
        try {
            return Optional.of(LocalDate.parse(regDate, ISO_DATE));
        } catch (DateTimeParseException ignored) {
            Matcher matcher = DATE_PATTERN.matcher(regDate);
            if (!matcher.find()) {
                return Optional.empty();
            }
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            return Optional.of(LocalDate.of(year, month, day));
        }
    }

    private String toPromptSummary(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return "";
        }
        return rawText.length() <= SUMMARY_MAX_LENGTH
                ? rawText
                : rawText.substring(0, SUMMARY_MAX_LENGTH);
    }

    private static final class Attachment {
        private final String fileName;
        private final String url;

        private Attachment(String fileName, String url) {
            this.fileName = fileName;
            this.url = url;
        }

        private String getFileName() {
            return fileName;
        }

        private String getUrl() {
            return url;
        }
    }
}
