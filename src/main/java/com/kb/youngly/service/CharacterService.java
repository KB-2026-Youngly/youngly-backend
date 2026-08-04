package com.kb.youngly.service;

import com.kb.youngly.dto.character.CharacterDrawResponse;
import com.kb.youngly.dto.character.CharacterEquipResponse;
import com.kb.youngly.dto.character.OwnedCharacterResponse;
import com.kb.youngly.mapper.CharacterMapper;
import com.kb.youngly.vo.character.CharacterEquipVO;
import com.kb.youngly.vo.point.CollectibleItemVO;
import com.kb.youngly.vo.point.UserItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;

/**
 * 캐릭터 랜덤 획득, 보유 캐릭터 조회, 장착의 비즈니스 흐름을 담당한다.
 * 사용자가 캐릭터를 뽑거나 장착할 때 포인트, 보유 캐릭터, 장착 상태가 따로따로 변경되지 않도록 하나의 흐름으로 처리했습니다.
 */
@Service
public class CharacterService {

    private static final int DRAW_COST = 100;
    private static final String DRAW_HISTORY_CONTENT = "캐릭터 랜덤 획득";

    private final CharacterMapper characterMapper;
    private final PointService pointService;
    private final IntUnaryOperator randomIndexGenerator;

    @Autowired
    public CharacterService(CharacterMapper characterMapper, PointService pointService) {
        this(
                characterMapper,
                pointService,
                bound -> ThreadLocalRandom.current().nextInt(bound)
        );
    }

    CharacterService(CharacterMapper characterMapper,
                     PointService pointService,
                     IntUnaryOperator randomIndexGenerator) {
        this.characterMapper = characterMapper;
        this.pointService = pointService;
        this.randomIndexGenerator = randomIndexGenerator;
    }

    /**
     * 사용자 행을 잠근 뒤 미보유 캐릭터 중 하나를 동일 확률로 선택하고,
     * 포인트 차감·사용 내역 저장·캐릭터 보유 저장을 한 트랜잭션에서 수행한다.
     * 후보 없음, 포인트 부족, 보유 저장 실패 등 예외가 발생하면 전체 작업이 롤백된다.
     */
    @Transactional
    public CharacterDrawResponse drawCharacter(String userId) {
        String validUserId = requireUserId(userId);

        // 사용자 행 잠금으로 같은 사용자의 요청을 직렬화하고 복합 유니크 키로 중복 저장을 막는다.
        if (characterMapper.lockUserForUpdate(validUserId) == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 잠금 이후 미보유 캐릭터를 조회하며, 후보가 없으면 포인트 차감 전에 종료한다.
        List<CollectibleItemVO> candidates =
                characterMapper.findUnownedCharacters(validUserId);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("모든 캐릭터를 이미 보유하고 있습니다.");
        }

        // 후보 목록의 인덱스를 균등하게 선택해 모든 미보유 캐릭터에 같은 확률을 적용한다.
        int selectedIndex = randomIndexGenerator.applyAsInt(candidates.size());
        if (selectedIndex < 0 || selectedIndex >= candidates.size()) {
            throw new IllegalStateException("캐릭터 추첨에 실패했습니다.");
        }
        CollectibleItemVO selectedCharacter = candidates.get(selectedIndex);

        // 기존 포인트 서비스를 호출해 잔액 차감과 USE 내역 저장을 현재 트랜잭션에 포함한다.
        pointService.usePoint(
                validUserId,
                selectedCharacter.getItemId(),
                DRAW_COST,
                DRAW_HISTORY_CONTENT
        );

        // 신규 캐릭터는 미장착 상태로 저장하며, 저장 실패 시 앞선 포인트 처리도 함께 롤백된다.
        UserItemVO userItem = UserItemVO.builder()
                .itemId(selectedCharacter.getItemId())
                .userId(validUserId)
                .isEquipped(false)
                .build();
        if (characterMapper.insertUserItem(userItem) != 1) {
            throw new IllegalStateException("캐릭터 보유 정보 저장에 실패했습니다.");
        }

        long remainingPoint = pointService.getPoint(validUserId);
        return CharacterDrawResponse.from(selectedCharacter, remainingPoint);
    }

    /**
     * 사용자의 보유 캐릭터 조회 결과를 외부 응답 DTO로 변환해 반환한다.
     */
    @Transactional(readOnly = true)
    public List<OwnedCharacterResponse> getOwnedCharacters(String userId) {
        String validUserId = requireUserId(userId);
        return characterMapper.findOwnedCharacters(validUserId)
                .stream()
                .map(OwnedCharacterResponse::from)
                .toList();
    }

    /**
     * 사용자가 보유한 캐릭터인지 확인한 뒤 다른 캐릭터의 장착을 해제하고 대상을 장착한다.
     * 이미 장착된 캐릭터는 그대로 유지하며, 해제와 장착 중 실패하면 전체 작업이 롤백된다.
     */
    @Transactional
    public CharacterEquipResponse equipCharacter(String userId, Long characterId) {
        String validUserId = requireUserId(userId);
        Long validCharacterId = requireCharacterId(characterId);

        // 같은 사용자의 뽑기·장착 요청이 동시에 보유 상태를 변경하지 않도록 직렬화한다.
        if (characterMapper.lockUserForUpdate(validUserId) == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 사용자와 캐릭터 ID를 함께 조회해 다른 사용자의 보유 캐릭터 접근도 차단한다.
        CharacterEquipVO character = characterMapper.findOwnedCharacterForEquip(
                validUserId,
                validCharacterId
        );
        if (character == null) {
            throw new IllegalArgumentException("보유하지 않은 캐릭터입니다.");
        }

        // 데이터 이상으로 여러 캐릭터가 장착된 경우에도 대상 외의 장착 상태를 모두 정리한다.
        characterMapper.unequipOtherCharacters(validUserId, validCharacterId);

        if (!Boolean.TRUE.equals(character.getEquipped())
                && characterMapper.equipCharacter(validUserId, validCharacterId) != 1) {
            throw new IllegalStateException("캐릭터 장착에 실패했습니다.");
        }

        return CharacterEquipResponse.fromEquipped(character);
    }

    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("인증된 사용자 정보가 없습니다.");
        }
        return userId;
    }

    private Long requireCharacterId(Long characterId) {
        if (characterId == null || characterId <= 0) {
            throw new IllegalArgumentException("캐릭터 ID는 1 이상이어야 합니다.");
        }
        return characterId;
    }
}
