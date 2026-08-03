package com.kb.youngly.dto.character;

import com.kb.youngly.vo.point.CollectibleItemVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 캐릭터 랜덤 획득 API의 외부 응답 DTO이다.
 * 획득한 캐릭터의 공개 정보와 포인트 차감 후 잔액만 노출한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterDrawResponse {

    private Long characterId;
    private String name;
    private String imageUrl;
    private long remainingPoint;

    public static CharacterDrawResponse from(CollectibleItemVO character, long remainingPoint) {
        return CharacterDrawResponse.builder()
                .characterId(character.getItemId())
                .name(character.getItemName())
                .imageUrl(character.getImageUrl())
                .remainingPoint(remainingPoint)
                .build();
    }
}
