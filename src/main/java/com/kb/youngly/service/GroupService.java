package com.kb.youngly.service;

import com.kb.youngly.dto.group.CreateGroupRequest;
import com.kb.youngly.dto.group.CreateGroupResponse;
import com.kb.youngly.dto.group.GroupListResponse;

import java.util.List;

public interface GroupService {

    /**
     * 그룹 생성
     */
    CreateGroupResponse createGroup(String userId,
                                    CreateGroupRequest request);

    /**
     * 그룹 조회
     */
    List<GroupListResponse> getGroupList(String userId);
}