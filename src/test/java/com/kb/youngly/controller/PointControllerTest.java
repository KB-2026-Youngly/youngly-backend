package com.kb.youngly.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.youngly.exception.CommonExceptionAdvice;
import com.kb.youngly.service.PointService;
import com.kb.youngly.support.InMemoryPointMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PointControllerTest {

    private static final String USER_ID = "authenticated-user-id";

    private InMemoryPointMapper pointMapper;
    private PointService pointService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        pointMapper = new InMemoryPointMapper();
        pointMapper.addUser(USER_ID, 1_000L);
        pointService = new PointService(pointMapper);

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = standaloneSetup(new PointController(pointService))
                .setControllerAdvice(new CommonExceptionAdvice())
                .setMessageConverters(converter)
                .build();
        authentication = new UsernamePasswordAuthenticationToken(
                USER_ID,
                null,
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("인증된 userId로 포인트를 지급하고 지급 결과를 응답한다")
    void earnPoint_usesAuthenticatedUserId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/points/earn")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 120,
                                  "content": "챌린지 인증 보상"
                                }
                                """))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = readResponseBody(result);
        assertEquals(1L, body.get("pointLogId").asLong());
        assertEquals("EARN", body.get("pointType").asText());
        assertEquals(120, body.get("amount").asInt());
        assertEquals("챌린지 인증 보상", body.get("content").asText());
        assertFalse(body.has("balance"));
        assertEquals(USER_ID, pointMapper.getHistories().get(0).getUserId());
    }

    @Test
    @DisplayName("인증된 사용자의 현재 포인트를 조회한다")
    void getBalance_returnsAuthenticatedUsersBalance() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/points/balance")
                        .principal(authentication))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = readResponseBody(result);
        assertEquals(1_000L, body.get("balance").asLong());
    }

    @Test
    @DisplayName("포인트 내역을 최신순으로 limit과 offset에 맞춰 조회한다")
    void getHistory_appliesLimitAndOffset() throws Exception {
        pointService.earnPoint(USER_ID, 100, "첫 번째 내역");
        pointService.earnPoint(USER_ID, 200, "두 번째 내역");

        MvcResult result = mockMvc.perform(get("/api/points/history")
                        .principal(authentication)
                        .param("limit", "1")
                        .param("offset", "1"))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = readResponseBody(result);
        assertEquals(1, body.size());
        assertEquals("첫 번째 내역", body.get(0).get("content").asText());
        assertEquals(1, pointMapper.getLastHistoryLimit());
        assertEquals(1, pointMapper.getLastHistoryOffset());
    }

    @Test
    @DisplayName("내역 조회 쿼리를 생략하면 limit 20과 offset 0을 사용한다")
    void getHistory_usesDefaultPaging() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/points/history")
                        .principal(authentication))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals(20, pointMapper.getLastHistoryLimit());
        assertEquals(0, pointMapper.getLastHistoryOffset());
    }

    @Test
    @DisplayName("10의 배수가 아닌 지급 요청은 공통 400 예외 형식으로 응답한다")
    void earnPoint_rejectsInvalidAmountWithCommonErrorResponse() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/points/earn")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 15,
                                  "content": "잘못된 지급"
                                }
                                """))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        JsonNode body = readResponseBody(result);
        assertEquals(400, body.get("status").asInt());
        assertEquals("Bad Request", body.get("error").asText());
        assertEquals("포인트는 10의 배수여야 합니다.", body.get("message").asText());
        assertEquals("/api/points/earn", body.get("path").asText());
        assertEquals(1_000L, pointMapper.getBalance(USER_ID));
        assertEquals(0, pointMapper.getInsertCallCount());
    }

    @Test
    @DisplayName("잘못된 내역 페이징은 공통 400 예외 형식으로 응답한다")
    void getHistory_rejectsInvalidPaging() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/points/history")
                        .principal(authentication)
                        .param("limit", "0")
                        .param("offset", "0"))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        JsonNode body = readResponseBody(result);
        assertEquals("조회 개수는 1 이상 100 이하이어야 합니다.", body.get("message").asText());
    }

    private JsonNode readResponseBody(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
    }
}
