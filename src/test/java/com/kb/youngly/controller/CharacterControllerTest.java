package com.kb.youngly.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.youngly.enums.ItemCategory;
import com.kb.youngly.exception.CommonExceptionAdvice;
import com.kb.youngly.service.CharacterService;
import com.kb.youngly.service.PointService;
import com.kb.youngly.support.InMemoryCharacterMapper;
import com.kb.youngly.support.InMemoryPointMapper;
import com.kb.youngly.vo.point.CollectibleItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class CharacterControllerTest {

    private static final String USER_ID = "authenticated-character-user";

    private InMemoryCharacterMapper characterMapper;
    private InMemoryPointMapper pointMapper;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        characterMapper = new InMemoryCharacterMapper();
        characterMapper.addUser(USER_ID);
        pointMapper = new InMemoryPointMapper();
        pointMapper.addUser(USER_ID, 500L);

        CharacterService characterService = new CharacterService(
                characterMapper,
                new PointService(pointMapper)
        );
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = standaloneSetup(new CharacterController(characterService))
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
    @DisplayName("요청 본문 없이 인증 사용자 기준으로 캐릭터를 획득한다")
    void drawCharacter_usesAuthenticatedUserWithoutRequestBody() throws Exception {
        characterMapper.addItem(character(1L, "키키"));

        MvcResult result = mockMvc.perform(post("/api/characters/draw")
                        .principal(authentication))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = readResponseBody(result);
        assertEquals(1L, body.get("characterId").asLong());
        assertEquals("키키", body.get("name").asText());
        assertEquals("/characters/1.png", body.get("imageUrl").asText());
        assertEquals(400L, body.get("remainingPoint").asLong());
        assertFalse(body.has("userId"));
        assertFalse(body.has("userItemId"));
        assertFalse(body.has("isEquipped"));
        assertEquals(USER_ID, characterMapper.getUserItems().get(0).getUserId());
    }

    @Test
    @DisplayName("인증 사용자의 보유 캐릭터만 최근 획득순으로 반환한다")
    void getOwnedCharacters_returnsPublicFieldsNewestFirst() throws Exception {
        characterMapper.addItem(character(1L, "먼저 획득"));
        characterMapper.addItem(character(2L, "나중 획득"));
        characterMapper.addOwnedItem(
                USER_ID,
                1L,
                LocalDateTime.of(2026, 8, 1, 10, 0),
                false
        );
        characterMapper.addOwnedItem(
                USER_ID,
                2L,
                LocalDateTime.of(2026, 8, 2, 10, 0),
                true
        );

        MvcResult result = mockMvc.perform(get("/api/characters/owned")
                        .principal(authentication))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = readResponseBody(result);
        assertEquals(2, body.size());
        assertEquals(2L, body.get(0).get("characterId").asLong());
        assertEquals("2026-08-02T10:00:00", body.get(0).get("acquiredAt").asText());
        assertTrue(body.get(0).get("equipped").asBoolean());
        assertEquals(1L, body.get(1).get("characterId").asLong());
        assertFalse(body.get(1).get("equipped").asBoolean());
        assertFalse(body.get(0).has("userId"));
        assertFalse(body.get(0).has("userItemId"));
        assertFalse(body.get(0).has("dropRate"));
        assertFalse(body.get(0).has("baseCharacter"));
    }

    @Test
    @DisplayName("캐릭터 획득 요청에 인증 정보가 없으면 실패한다")
    void drawCharacter_withoutAuthenticationFails() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/characters/draw"))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals(
                "인증된 사용자 정보가 없습니다.",
                readResponseBody(result).get("message").asText()
        );
        assertEquals(0, pointMapper.getDecreaseCallCount());
    }

    @Test
    @DisplayName("보유 캐릭터 조회에 인증 정보가 없으면 실패한다")
    void getOwnedCharacters_withoutAuthenticationFails() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/characters/owned"))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals(
                "인증된 사용자 정보가 없습니다.",
                readResponseBody(result).get("message").asText()
        );
    }

    @Test
    @DisplayName("인증 사용자가 보유한 캐릭터를 장착한다")
    void equipCharacter_usesAuthenticatedUser() throws Exception {
        characterMapper.addItem(character(1L, "키키"));
        characterMapper.addOwnedItem(USER_ID, 1L, LocalDateTime.now());

        MvcResult result = mockMvc.perform(put("/api/characters/1/equip")
                        .principal(authentication))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = readResponseBody(result);
        assertEquals(1L, body.get("characterId").asLong());
        assertEquals("키키", body.get("name").asText());
        assertEquals("/characters/1.png", body.get("imageUrl").asText());
        assertTrue(body.get("equipped").asBoolean());
        assertFalse(body.has("userId"));
        assertFalse(body.has("userItemId"));
        assertTrue(characterMapper.getUserItems().get(0).getIsEquipped());
    }

    @Test
    @DisplayName("캐릭터 장착 요청에 인증 정보가 없으면 실패한다")
    void equipCharacter_withoutAuthenticationFails() throws Exception {
        characterMapper.addItem(character(1L, "키키"));
        characterMapper.addOwnedItem(USER_ID, 1L, LocalDateTime.now());

        MvcResult result = mockMvc.perform(put("/api/characters/1/equip"))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals(
                "인증된 사용자 정보가 없습니다.",
                readResponseBody(result).get("message").asText()
        );
        assertFalse(characterMapper.getUserItems().get(0).getIsEquipped());
    }

    private JsonNode readResponseBody(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
    }

    private CollectibleItemVO character(Long itemId, String name) {
        return CollectibleItemVO.builder()
                .itemId(itemId)
                .itemCategory(ItemCategory.CHARACTER)
                .itemName(name)
                .imageUrl("/characters/" + itemId + ".png")
                .build();
    }
}
