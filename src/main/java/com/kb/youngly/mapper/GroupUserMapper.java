package com.kb.youngly.mapper;

import com.kb.youngly.vo.group.GroupUserVO;
import org.apache.ibatis.annotations.Param;

public interface GroupUserMapper {

    GroupUserVO findGroupUser(
            @Param("groupId") String groupId,
            @Param("userId") String userId);

    void insertGroupUser(GroupUserVO groupUser);

}