package com.kb.youngly.client;

import com.kb.youngly.dto.market.FssMarketItem;
import com.kb.youngly.properties.FssMarketProperties;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 금감원 금융시장동향 Open API 실제 호출 통합 테스트.
 *
 * 실제 금감원 금융시장동향 API 통합 테스트.
 */
@Log4j2
@Tag("integration")
class FssMarketApiClientTest {

    private FssMarketApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        Properties props = loadApplicationProperties();

        FssMarketProperties properties = new FssMarketProperties();
        ReflectionTestUtils.setField(properties, "baseUrl",
                props.getProperty("fss.market.base-url",
                        "https://www.fss.or.kr/fss/kr/openApi/api/fnncMrkt.jsp"));
        ReflectionTestUtils.setField(properties, "apiType",
                props.getProperty("fss.market.api-type", "json"));
        ReflectionTestUtils.setField(properties, "authKey",
                props.getProperty("fss.market.auth-key", ""));

        client = new FssMarketApiClient(properties);
    }

    @Test
    @DisplayName("실제 API 호출 시 금융시장동향 목록을 반환한다")
    void fetchMarketItems_realCall_returnsItems() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(14);

        log.info("[INFO] real call range: {} ~ {}", startDate, endDate);

        List<FssMarketItem> items = client.fetchMarketItems(startDate, endDate);

        assertNotNull(items, "응답 리스트는 null 이면 안 됩니다.");
        assertFalse(items.isEmpty(), "최근 14일 금융시장동향 결과가 비어 있으면 안 됩니다.");

        FssMarketItem first = items.get(0);
        assertTrue(StringUtils.hasText(first.getSubject()), "subject 가 있어야 합니다.");
        assertTrue(StringUtils.hasText(first.getRegDate()), "regDate 가 있어야 합니다.");
        assertTrue(items.stream().anyMatch(item ->
                StringUtils.hasText(item.getAtchfileNm()) && item.getAtchfileNm().contains("오후동향")),
                "오후동향 PDF 첨부파일이 최소 1건 있어야 합니다.");

        items.forEach(item -> log.info(
                "[INFO] subject={}, regDate={}, atchfileNm={}, atchfileUrl={}",
                item.getSubject(), item.getRegDate(), item.getAtchfileNm(), item.getAtchfileUrl()));
    }

    private static Properties loadApplicationProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream in = FssMarketApiClientTest.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in == null) {
                throw new IOException("classpath:/application.properties 를 찾을 수 없습니다.");
            }
            props.load(in);
        }
        return props;
    }
}
