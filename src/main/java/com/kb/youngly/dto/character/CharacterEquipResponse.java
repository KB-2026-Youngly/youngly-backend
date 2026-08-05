package com.kb.youngly.dto.character;

import com.kb.youngly.vo.character.CharacterEquipVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 캐릭터 장착 API의 외부 응답 DTO이다.
 * 장착한 캐릭터의 공개 정보와 장착 여부만 노출한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterEquipResponse {

    private Long characterId;
    private String name;
    private String imageUrl;
    private boolean equipped;

    public static CharacterEquipResponse fromEquipped(CharacterEquipVO character) {
        return CharacterEquipResponse.builder()
                .characterId(character.getCharacterId())
                .name(character.getName())
                .imageUrl(character.getImageUrl())
                .equipped(true)
                .build();
    }
}
