package com.kb.youngly.service;

import com.kb.youngly.exception.FssMarketPdfDownloadException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;

@Service
public class FssMarketPdfDownloadService {

    private static final Logger log = LogManager.getLogger(FssMarketPdfDownloadService.class);
    private static final String FSS_ORIGIN = "https://www.fss.or.kr";
    private static final int TIMEOUT_MILLIS = 7_000;

    public byte[] download(String pdfUrl) {
        if (!StringUtils.hasText(pdfUrl)) {
            throw new FssMarketPdfDownloadException("PDF URL이 비어 있습니다.");
        }

        URI uri = URI.create(resolveUrl(pdfUrl));
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_MILLIS)
                .setConnectionRequestTimeout(TIMEOUT_MILLIS)
                .setSocketTimeout(TIMEOUT_MILLIS)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
             CloseableHttpResponse response = httpClient.execute(new HttpGet(uri))) {

            int status = response.getStatusLine().getStatusCode();
            byte[] body = response.getEntity() == null ? new byte[0] : EntityUtils.toByteArray(response.getEntity());

            if (status < 200 || status >= 300 || body.length == 0) {
                throw new FssMarketPdfDownloadException(
                        "PDF 다운로드 응답이 유효하지 않습니다. status=" + status + ", url=" + uri);
            }

            return body;
        } catch (IOException e) {
            log.error("[ERROR] 금감원 금융시장동향 PDF 다운로드 실패. pdfUrl={}", uriOrOriginal(pdfUrl), e);
            throw new FssMarketPdfDownloadException("금감원 금융시장동향 PDF 다운로드에 실패했습니다.", e);
        }
    }

    private String resolveUrl(String pdfUrl) {
        String trimmed = pdfUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("//")) {
            return "https:" + trimmed;
        }
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        return FSS_ORIGIN + trimmed;
    }

    private String uriOrOriginal(String pdfUrl) {
        try {
            return resolveUrl(pdfUrl);
        } catch (RuntimeException ignored) {
            return pdfUrl;
        }
    }
}
