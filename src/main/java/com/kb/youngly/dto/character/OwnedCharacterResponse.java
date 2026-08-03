package com.kb.youngly.dto.character;

import com.kb.youngly.vo.character.OwnedCharacterVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
