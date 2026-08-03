package com.kb.youngly.mapper;

import com.kb.youngly.vo.CollectibleItemVO;
import com.kb.youngly.vo.UserItemVO;
import com.kb.youngly.vo.character.OwnedCharacterVO;
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
