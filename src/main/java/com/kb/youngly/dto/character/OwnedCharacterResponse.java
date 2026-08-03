package com.kb.youngly.dto.character;

import com.kb.youngly.vo.character.OwnedCharacterVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 보유 캐릭터 목록 API의 외부 응답 DTO이다.
 * 캐릭터 공개 정보와 획득 일시만 노출한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnedCharacterResponse {

    private Long characterId;
    private String name;
    private String imageUrl;
    private LocalDateTime acquiredAt;

    public static OwnedCharacterResponse from(OwnedCharacterVO character) {
        return OwnedCharacterResponse.builder()
                .characterId(character.getCharacterId())
                .name(character.getName())
                .imageUrl(character.getImageUrl())
                .acquiredAt(character.getAcquiredAt())
                .build();
    }
}
