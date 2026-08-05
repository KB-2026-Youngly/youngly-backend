package com.kb.youngly.mapper;

import com.kb.youngly.dto.group.JoinRequestResponse;
import com.kb.youngly.enums.GroupUserStatus;
import com.kb.youngly.vo.group.GroupUserVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupUserMapper {

    GroupUserVO findGroupUser(
            @Param("groupId") String groupId,
            @Param("userId") String userId);

    void insertGroupUser(GroupUserVO groupUser);

    //승인 대기 목록 조회
    List<JoinRequestResponse> findPendingGroupUsers(String groupId);

    // groupUserId로 참여 정보 조회
    GroupUserVO findGroupUserById(Long groupUserId);

    // 참여 승인
    void updateGroupUserStatus(
            @Param("groupUserId") Long groupUserId,
            @Param("status") GroupUserStatus status);
}