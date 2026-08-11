package com.kb.youngly.service;

import com.kb.youngly.dto.character.CharacterDrawResponse;
import com.kb.youngly.dto.character.CharacterEquipResponse;
import com.kb.youngly.dto.character.OwnedCharacterResponse;
import com.kb.youngly.enums.ItemCategory;
import com.kb.youngly.enums.PointType;
import com.kb.youngly.support.InMemoryCharacterMapper;
import com.kb.youngly.support.InMemoryPointMapper;
import com.kb.youngly.vo.point.CollectibleItemVO;
import com.kb.youngly.vo.point.PointHistoryVO;
import com.kb.youngly.vo.point.UserItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterServiceTest {

    private static final String USER_ID = "character-user";
    private static final String OTHER_USER_ID = "other-character-user";

    private InMemoryCharacterMapper characterMapper;
    private InMemoryPointMapper pointMapper;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        characterMapper = new InMemoryCharacterMapper();
        characterMapper.addUser(USER_ID);
        pointMapper = new InMemoryPointMapper();
        pointMapper.addUser(USER_ID, 500L);
        pointService = new PointService(pointMapper);
    }

    @Test
    @DisplayName("랜덤 캐릭터를 획득하면 100포인트와 사용 내역을 저장한다")
    void drawCharacter_deductsPointAndStoresOwnership() {
        characterMapper.addItem(character(1L, "키키"));
        CharacterService service = serviceWithFixedIndex(0);

        CharacterDrawResponse response = service.drawCharacter(USER_ID);

        assertEquals(1L, response.getCharacterId());
        assertEquals("키키", response.getName());
        assertEquals("/characters/1.png", response.getImageUrl());
        assertEquals(400L, response.getRemainingPoint());
        assertEquals(400L, pointMapper.getBalance(USER_ID));

        List<PointHistoryVO> histories = pointMapper.getHistories();
        assertEquals(1, histories.size());
        assertEquals(PointType.USE, histories.get(0).getPointType());
        assertEquals(100, histories.get(0).getAmount());
        assertEquals(1L, histories.get(0).getItemId());

        List<UserItemVO> userItems = characterMapper.getUserItems();
        assertEquals(1, userItems.size());
        assertEquals(USER_ID, userItems.get(0).getUserId());
        assertEquals(1L, userItems.get(0).getItemId());
        assertFalse(userItems.get(0).getIsEquipped());
        assertEquals(
                List.of("lockUserForUpdate", "findUnownedCharacters", "insertUserItem"),
                characterMapper.getCallLog()
        );
    }

    @Test
    @DisplayName("이미 보유한 캐릭터와 비캐릭터는 추첨 대상에서 제외한다")
    void drawCharacter_selectsOnlyUnownedCharacters() {
        characterMapper.addItem(character(1L, "보유 캐릭터"));
        characterMapper.addItem(character(2L, "미보유 캐릭터 A"));
        characterMapper.addItem(character(3L, "미보유 캐릭터 B"));
        characterMapper.addItem(item(4L, "액세서리", ItemCategory.ACC));
        characterMapper.addOwnedItem(USER_ID, 1L, LocalDateTime.now().minusDays(1));

        AtomicInteger candidateCount = new AtomicInteger();
        CharacterService service = new CharacterService(
                characterMapper,
                pointService,
                bound -> {
                    candidateCount.set(bound);
                    return 1;
                }
        );

        CharacterDrawResponse response = service.drawCharacter(USER_ID);

        assertEquals(2, candidateCount.get());
        assertEquals(3L, response.getCharacterId());
        assertTrue(characterMapper.getUserItems().stream()
                .noneMatch(userItem -> userItem.getItemId().equals(4L)));
    }

    @Test
    @DisplayName("포인트가 부족하면 포인트 내역과 보유 캐릭터를 저장하지 않는다")
    void drawCharacter_insufficientPointChangesNothing() {
        pointMapper = new InMemoryPointMapper();
        pointMapper.addUser(USER_ID, 90L);
        pointService = new PointService(pointMapper);
        characterMapper.addItem(character(1L, "키키"));
        CharacterService service = serviceWithFixedIndex(0);

        assertThrows(IllegalArgumentException.class, () -> service.drawCharacter(USER_ID));

        assertEquals(90L, pointMapper.getBalance(USER_ID));
        assertEquals(0, pointMapper.getHistories().size());
        assertEquals(0, characterMapper.getUserItems().size());
        assertEquals(1, pointMapper.getDecreaseCallCount());
        assertEquals(0, pointMapper.getInsertCallCount());
    }

    @Test
    @DisplayName("모든 캐릭터를 보유하면 포인트 차감 전에 실패한다")
    void drawCharacter_allOwnedDoesNotUsePoint() {
        characterMapper.addItem(character(1L, "키키"));
        characterMapper.addOwnedItem(USER_ID, 1L, LocalDateTime.now());
        CharacterService service = serviceWithFixedIndex(0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.drawCharacter(USER_ID)
        );

        assertEquals("모든 캐릭터를 이미 보유하고 있습니다.", exception.getMessage());
        assertEquals(500L, pointMapper.getBalance(USER_ID));
        assertEquals(0, pointMapper.getDecreaseCallCount());
        assertEquals(0, pointMapper.getInsertCallCount());
        assertEquals(1, characterMapper.getUserItems().size());
    }

    @Test
    @DisplayName("보유 캐릭터를 최근 획득순으로 조회한다")
    void getOwnedCharacters_returnsNewestFirst() {
        characterMapper.addItem(character(1L, "먼저 획득"));
        characterMapper.addItem(character(2L, "나중 획득"));
        characterMapper.addItem(item(3L, "프레임", ItemCategory.FRAME));
        LocalDateTime older = LocalDateTime.of(2026, 8, 1, 10, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 8, 2, 10, 0);
        characterMapper.addOwnedItem(USER_ID, 1L, older, false);
        characterMapper.addOwnedItem(USER_ID, 2L, newer, true);
        characterMapper.addOwnedItem(USER_ID, 3L, newer.plusHours(1));

        List<OwnedCharacterResponse> response =
                serviceWithFixedIndex(0).getOwnedCharacters(USER_ID);

        assertEquals(2, response.size());
        assertEquals(2L, response.get(0).getCharacterId());
        assertEquals(newer, response.get(0).getAcquiredAt());
        assertTrue(response.get(0).isEquipped());
        assertEquals(1L, response.get(1).getCharacterId());
        assertEquals(older, response.get(1).getAcquiredAt());
        assertFalse(response.get(1).isEquipped());
    }

    @Test
    @DisplayName("보유 캐릭터를 정상 장착한다")
    void equipCharacter_equipsOwnedCharacter() {
        characterMapper.addItem(character(1L, "키키"));
        characterMapper.addOwnedItem(USER_ID, 1L, LocalDateTime.now());

        CharacterEquipResponse response = serviceWithFixedIndex(0)
                .equipCharacter(USER_ID, 1L);

        assertEquals(1L, response.getCharacterId());
        assertEquals("키키", response.getName());
        assertEquals("/characters/1.png", response.getImageUrl());
        assertTrue(response.isEquipped());
        assertTrue(userItem(USER_ID, 1L).getIsEquipped());
        assertEquals("/characters/1.png", characterMapper.getProfileImage(USER_ID));
        assertEquals(
                List.of(
                        "lockUserForUpdate",
                        "findOwnedCharacterForEquip",
                        "unequipOtherCharacters",
                        "equipCharacter",
                        "updateUserProfileImage"
                ),
                characterMapper.getCallLog()
        );
    }

    @Test
    @DisplayName("새 캐릭터를 장착하면 기존 캐릭터 장착을 해제한다")
    void equipCharacter_unequipsPreviouslyEquippedCharacter() {
        characterMapper.addItem(character(1L, "기존 장착"));
        characterMapper.addItem(character(2L, "새 장착"));
        characterMapper.addItem(item(3L, "장착 프레임", ItemCategory.FRAME));
        characterMapper.addItem(item(4L, "기존 장신구", ItemCategory.ACC));
        characterMapper.addOwnedItem(USER_ID, 1L, LocalDateTime.now().minusDays(1), true);
        characterMapper.addOwnedItem(USER_ID, 2L, LocalDateTime.now(), false);
        characterMapper.addOwnedItem(USER_ID, 3L, LocalDateTime.now(), true);
        characterMapper.addOwnedItem(USER_ID, 4L, LocalDateTime.now(), true);

        CharacterEquipResponse response = serviceWithFixedIndex(0)
                .equipCharacter(USER_ID, 2L);

        assertEquals(2L, response.getCharacterId());
        assertTrue(response.isEquipped());
        assertFalse(userItem(USER_ID, 1L).getIsEquipped());
        assertTrue(userItem(USER_ID, 2L).getIsEquipped());
        assertTrue(userItem(USER_ID, 3L).getIsEquipped());
        assertFalse(userItem(USER_ID, 4L).getIsEquipped());
        assertEquals("/characters/2.png", characterMapper.getProfileImage(USER_ID));
    }

    @Test
    @DisplayName("이미 장착된 캐릭터를 다시 요청해도 동일한 결과를 반환한다")
    void equipCharacter_alreadyEquippedIsIdempotent() {
        characterMapper.addItem(character(1L, "키키"));
        characterMapper.addOwnedItem(USER_ID, 1L, LocalDateTime.now(), true);

        CharacterEquipResponse response = serviceWithFixedIndex(0)
                .equipCharacter(USER_ID, 1L);

        assertEquals(1L, response.getCharacterId());
        assertTrue(response.isEquipped());
        assertTrue(userItem(USER_ID, 1L).getIsEquipped());
        assertFalse(characterMapper.getCallLog().contains("equipCharacter"));
        assertEquals("/characters/1.png", characterMapper.getProfileImage(USER_ID));
        assertTrue(characterMapper.getCallLog().contains("updateUserProfileImage"));
    }

    @Test
    @DisplayName("프로필 이미지 변경 실패는 장착 트랜잭션의 예외가 된다")
    void equipCharacter_profileImageUpdateFailureThrows() {
        characterMapper.addItem(character(1L, "키키"));
        characterMapper.addOwnedItem(USER_ID, 1L, LocalDateTime.now());
        characterMapper.setProfileImageUpdateResult(0);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> serviceWithFixedIndex(0).equipCharacter(USER_ID, 1L)
        );

        assertEquals("프로필 이미지 변경에 실패했습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("다른 사용자가 보유한 캐릭터는 장착할 수 없다")
    void equipCharacter_characterOwnedByAnotherUserFails() {
        characterMapper.addUser(OTHER_USER_ID);
        characterMapper.addItem(character(1L, "다른 사용자 캐릭터"));
        characterMapper.addOwnedItem(OTHER_USER_ID, 1L, LocalDateTime.now(), true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> serviceWithFixedIndex(0).equipCharacter(USER_ID, 1L)
        );

        assertEquals("보유하지 않은 캐릭터입니다.", exception.getMessage());
        assertTrue(userItem(OTHER_USER_ID, 1L).getIsEquipped());
        assertEquals(
                List.of("lockUserForUpdate", "findOwnedCharacterForEquip"),
                characterMapper.getCallLog()
        );
    }

    @Test
    @DisplayName("캐릭터 획득은 쓰기 트랜잭션으로 처리한다")
    void drawCharacter_isTransactional() throws NoSuchMethodException {
        Method method = CharacterService.class.getMethod("drawCharacter", String.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }

    @Test
    @DisplayName("보유 캐릭터 조회는 읽기 전용 트랜잭션으로 처리한다")
    void getOwnedCharacters_isReadOnlyTransactional() throws NoSuchMethodException {
        Method method = CharacterService.class.getMethod("getOwnedCharacters", String.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertTrue(transactional.readOnly());
    }

    @Test
    @DisplayName("캐릭터 장착과 기존 장착 해제는 쓰기 트랜잭션으로 처리한다")
    void equipCharacter_isTransactional() throws NoSuchMethodException {
        Method method = CharacterService.class.getMethod(
                "equipCharacter",
                String.class,
                Long.class
        );
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }

    private CharacterService serviceWithFixedIndex(int index) {
        return new CharacterService(characterMapper, pointService, bound -> index);
    }

    private UserItemVO userItem(String userId, Long itemId) {
        return characterMapper.getUserItems().stream()
                .filter(item -> userId.equals(item.getUserId()))
                .filter(item -> itemId.equals(item.getItemId()))
                .findFirst()
                .orElseThrow();
    }

    private CollectibleItemVO character(Long itemId, String name) {
        return item(itemId, name, ItemCategory.CHARACTER);
    }

    private CollectibleItemVO item(Long itemId, String name, ItemCategory category) {
        return CollectibleItemVO.builder()
                .itemId(itemId)
                .itemCategory(category)
                .itemName(name)
                .imageUrl("/characters/" + itemId + ".png")
                .build();
    }
}
