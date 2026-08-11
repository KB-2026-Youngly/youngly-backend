package com.kb.youngly.vo.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 보유 캐릭터와 아이템 정보를 조인한 Mapper 조회 결과를 담는 내부 VO이다.
 * API 응답에는 직접 사용하지 않고 {@code OwnedCharacterResponse}로 변환한다.
 */
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
    private boolean equipped;
}
