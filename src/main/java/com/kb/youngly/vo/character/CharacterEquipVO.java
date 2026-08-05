package com.kb.youngly.vo.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 장착 대상 캐릭터의 소유 정보와 현재 장착 상태를 담는 Mapper 내부 조회 VO이다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterEquipVO {

    private Long characterId;
    private String name;
    private String imageUrl;
    private Boolean equipped;
}
