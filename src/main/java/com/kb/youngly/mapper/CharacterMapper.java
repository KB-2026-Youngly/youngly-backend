package com.kb.youngly.mapper;

import com.kb.youngly.vo.character.OwnedCharacterVO;
import com.kb.youngly.vo.character.CharacterEquipVO;
import com.kb.youngly.vo.point.CollectibleItemVO;
import com.kb.youngly.vo.point.UserItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 캐릭터 뽑기, 보유 목록 조회, 장착에 필요한 SQL을 연결하는 MyBatis 매퍼이다.
 */
@Mapper
public interface CharacterMapper {

    /** 같은 사용자의 동시 뽑기를 직렬화하기 위해 사용자 행을 잠근다. */
    String lockUserForUpdate(@Param("userId") String userId);

    /** 사용자가 아직 보유하지 않은 캐릭터를 추첨 후보로 조회한다. */
    List<CollectibleItemVO> findUnownedCharacters(@Param("userId") String userId);

    /** 획득한 캐릭터를 사용자 보유 아이템으로 저장한다. */
    int insertUserItem(UserItemVO userItem);

    /** 사용자가 보유한 캐릭터를 최근 획득순으로 조회한다. */
    List<OwnedCharacterVO> findOwnedCharacters(@Param("userId") String userId);

    /** 사용자 소유 여부와 현재 장착 상태를 함께 확인한다. */
    CharacterEquipVO findOwnedCharacterForEquip(@Param("userId") String userId,
                                                @Param("characterId") Long characterId);

    /** 장착 대상 외에 사용자가 장착 중인 모든 캐릭터를 해제한다. */
    int unequipOtherCharacters(@Param("userId") String userId,
                               @Param("characterId") Long characterId);

    /** 사용자가 보유한 대상 캐릭터를 장착 상태로 변경한다. */
    int equipCharacter(@Param("userId") String userId,
                       @Param("characterId") Long characterId);
}
