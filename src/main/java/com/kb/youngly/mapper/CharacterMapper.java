package com.kb.youngly.mapper;

import com.kb.youngly.vo.character.OwnedCharacterVO;
import com.kb.youngly.vo.point.CollectibleItemVO;
import com.kb.youngly.vo.point.UserItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CharacterMapper {

    String lockUserForUpdate(@Param("userId") String userId);

    List<CollectibleItemVO> findUnownedCharacters(@Param("userId") String userId);

    int insertUserItem(UserItemVO userItem);

    List<OwnedCharacterVO> findOwnedCharacters(@Param("userId") String userId);
}
