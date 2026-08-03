package com.kb.youngly.vo.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnedCharacterVO {

    private Long characterId;
    private String name;
    private String imageUrl;
    private LocalDateTime acquiredAt;
}
