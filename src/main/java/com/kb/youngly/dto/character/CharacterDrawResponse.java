package com.kb.youngly.dto.character;

import com.kb.youngly.vo.point.CollectibleItemVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
