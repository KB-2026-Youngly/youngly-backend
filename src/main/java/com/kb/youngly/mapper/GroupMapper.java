package com.kb.youngly.mapper;

import com.kb.youngly.dto.group.GroupListResponse;
import com.kb.youngly.vo.group.GroupVO;
import java.util.List;

public interface GroupMapper {

    /**
     * 그룹 생성
     */
    void insertGroup(GroupVO group);

    /**
     * 그룹 조회
     */
    List<GroupListResponse> findGroupsByUserId(String userId);
}