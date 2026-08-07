package com.kb.youngly.service;

import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.group.*;

import java.util.List;

public interface GroupService {

    /**
     * 그룹 생성
     */
    CreateGroupResponse createGroup(String userId,
                                    CreateGroupRequest request);

    /**
     * 그룹 목록 조회
     */
    List<GroupListResponse> getGroupList(String userId);

    /**
     * 그룹 상세 조회
     */
    GroupDetailResponse getGroupDetail(
            String userId,
            String groupId);

    /**
     * 그룹 수정
     */
    MessageResponse updateGroup(
            String userId,
            String groupId,
            UpdateGroupRequest request);

    /**
     * 그룹 종료(삭제)
     */
    MessageResponse deleteGroup(
            String userId,
            String groupId);

    /**
     * 그룹 참여
     */
    MessageResponse joinGroup(
            String userId,
            JoinGroupRequest request);

    /**
     * 승인 대기 목록 조회
     */
    List<JoinRequestResponse> getJoinRequests(
            String userId,
            String groupId);

    // 승인
    MessageResponse approveJoinRequest(
            String userId,
            String groupId,
            Long groupUserId);

    // 거절
    MessageResponse rejectJoinRequest(
            String userId,
            String groupId,
            Long groupUserId);

    // 그룹 참여자 조회
    List<GroupUserResponse> getGroupUsers(
            String userId,
            String groupId);

    // 강퇴
    MessageResponse kickGroupUser(
            String userId,
            String groupId,
            Long groupUserId);

    // 탈퇴
    MessageResponse leaveGroup(
            String userId,
            String groupId);

    // 초대 코드 재발급
    RegenerateInviteCodeResponse regenerateInviteCode(
            String userId,
            String groupId);
}