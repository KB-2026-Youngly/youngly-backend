package com.kb.youngly.service;

import com.kb.youngly.dto.character.CharacterDrawResponse;
import com.kb.youngly.dto.character.OwnedCharacterResponse;
import com.kb.youngly.mapper.CharacterMapper;
import com.kb.youngly.vo.point.CollectibleItemVO;
import com.kb.youngly.vo.point.UserItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;

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

    @Transactional
    public CharacterDrawResponse drawCharacter(String userId) {
        String validUserId = requireUserId(userId);

        // 사용자 행 잠금으로 같은 사용자의 요청을 직렬화하고 복합 유니크 키로 중복 저장을 막는다.
        if (characterMapper.lockUserForUpdate(validUserId) == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        List<CollectibleItemVO> candidates =
                characterMapper.findUnownedCharacters(validUserId);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("모든 캐릭터를 이미 보유하고 있습니다.");
        }

        int selectedIndex = randomIndexGenerator.applyAsInt(candidates.size());
        if (selectedIndex < 0 || selectedIndex >= candidates.size()) {
            throw new IllegalStateException("캐릭터 추첨에 실패했습니다.");
        }
        CollectibleItemVO selectedCharacter = candidates.get(selectedIndex);

        pointService.usePoint(
                validUserId,
                selectedCharacter.getItemId(),
                DRAW_COST,
                DRAW_HISTORY_CONTENT
        );

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

    @Transactional(readOnly = true)
    public List<OwnedCharacterResponse> getOwnedCharacters(String userId) {
        String validUserId = requireUserId(userId);
        return characterMapper.findOwnedCharacters(validUserId)
                .stream()
                .map(OwnedCharacterResponse::from)
                .toList();
    }

    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("인증된 사용자 정보가 없습니다.");
        }
        return userId;
    }
}
