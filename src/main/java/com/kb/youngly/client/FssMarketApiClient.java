package com.kb.youngly.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.youngly.dto.market.FssMarketApiResponse;
import com.kb.youngly.dto.market.FssMarketItem;
import com.kb.youngly.exception.FssMarketApiException;
import com.kb.youngly.exception.FssMarketRateLimitException;
import com.kb.youngly.properties.FssMarketProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Component
public class FssMarketApiClient {

    private static final Logger log = LogManager.getLogger(FssMarketApiClient.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int TIMEOUT_MILLIS = 7_000;

    private final FssMarketProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FssMarketApiClient(FssMarketProperties properties) {
        this.properties = properties;
    }

    public List<FssMarketItem> fetchMarketItems(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
        if (!StringUtils.hasText(properties.getAuthKey())) {
            throw new FssMarketApiException("fss.market.auth-key 가 설정되지 않았습니다.");
        }

        URI uri = URI.create(properties.getBaseUrl()
                + "?apiType=" + encode(properties.getApiType())
                + "&startDate=" + startDate.format(DATE_FORMAT)
                + "&endDate=" + endDate.format(DATE_FORMAT)
                + "&authKey=" + encode(properties.getAuthKey()));

        log.info("[INFO] 금감원 금융시장동향 API 호출. startDate={}, endDate={}", startDate, endDate);

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
            String body = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());

            if (status < 200 || status >= 300) {
                throw new FssMarketApiException(
                        "금감원 금융시장동향 API 호출이 실패했습니다. status=" + status + ", body=" + snippet(body));
            }
            if (!StringUtils.hasText(body)) {
                throw new FssMarketApiException("금감원 금융시장동향 API 응답이 비어 있습니다.");
            }

            FssMarketApiResponse apiResponse = objectMapper.readValue(body, FssMarketApiResponse.class);
            if (apiResponse.getResponse() == null) {
                throw new FssMarketApiException("금감원 금융시장동향 API 응답에 reponse 바디가 없습니다.");
            }
            if (!apiResponse.isSuccessful()) {
                if (FssMarketRateLimitException.RESULT_CODE.equals(apiResponse.getResultCode())) {
                    throw new FssMarketRateLimitException(
                            "금감원 금융시장동향 API 일일 조회 한도에 도달했습니다. resultCode="
                                    + apiResponse.getResultCode()
                                    + ", resultMsg="
                                    + apiResponse.getResultMsg());
                }
                throw new FssMarketApiException(
                        "금감원 금융시장동향 API가 실패 응답을 반환했습니다. resultCode="
                                + apiResponse.getResultCode()
                                + ", resultMsg="
                                + apiResponse.getResultMsg());
            }

            List<FssMarketItem> items = apiResponse.resolveItems();
            log.info("[INFO] 금감원 API 조회 완료. resultCode={}, resultCnt={}, items={}",
                    apiResponse.getResultCode(), apiResponse.resolveResultCnt(), items.size());
            return items;
        } catch (JsonProcessingException e) {
            log.error("[ERROR] 금감원 API JSON 파싱 실패. startDate={}, endDate={}", startDate, endDate, e);
            throw new FssMarketApiException("금감원 금융시장동향 API 응답 파싱에 실패했습니다.", e);
        } catch (IOException e) {
            log.error("[ERROR] 금감원 API 호출 실패. startDate={}, endDate={}", startDate, endDate, e);
            throw new FssMarketApiException("금감원 금융시장동향 API 호출에 실패했습니다.", e);
        }
    }

    private String encode(String value) {
        return value == null ? "" : java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String snippet(String body) {
        if (!StringUtils.hasText(body)) {
            return "";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(0, 500);
    }
}
