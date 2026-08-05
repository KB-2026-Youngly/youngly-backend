package com.kb.youngly.service.impl;

import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.group.*;
import com.kb.youngly.enums.GroupStatus;
import com.kb.youngly.mapper.GroupMapper;
import com.kb.youngly.service.GroupService;
import com.kb.youngly.vo.group.GroupVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupMapper groupMapper;

    public GroupServiceImpl(GroupMapper groupMapper) {
        this.groupMapper = groupMapper;
    }

    // 그룹 생성
    @Override
    public CreateGroupResponse createGroup(String userId,
                                           CreateGroupRequest request) {

        GroupVO group = GroupVO.builder()
                .groupId(UUID.randomUUID().toString())
                .moimAccountId(request.getMoimAccountId())
                .userId(userId)
                .inviteCode(UUID.randomUUID().toString())
                .groupName(request.getGroupName())
                .groupCount(request.getGroupCount())
                .customRule(request.getCustomRule())
                .challengeType(request.getChallengeType())
                .content(request.getContent())
                .futureDepositRatioRule(request.getFutureDepositRatioRule())
                .durationDays(request.getDurationDays())
                .minCount(request.getMinCount())
                .roundCycleDays(request.getRoundCycleDays())
                .baseDepositAmount(request.getBaseDepositAmount())
                .groupStatus(GroupStatus.RECRUITING)
                .build();

        groupMapper.insertGroup(group);

        return CreateGroupResponse.builder()
                .groupId(group.getGroupId())
                .inviteCode(group.getInviteCode())
                .build();
    }

    // 그룹 목록 조회
    @Override
    public List<GroupListResponse> getGroupList(String userId) {

        return groupMapper.findGroupsByUserId(userId);
    }

    // 그룹 상세 조회
    @Override
    public GroupDetailResponse getGroupDetail(String userId,
                                              String groupId) {

        GroupVO group = groupMapper.findGroupById(groupId);

        if (group == null) {
            throw new IllegalArgumentException("존재하지 않는 그룹입니다.");
        }

        return GroupDetailResponse.builder()
                .groupId(group.getGroupId())
                .inviteCode(
                        userId.equals(group.getUserId())
                                ? group.getInviteCode()
                                : null
                )
                .groupName(group.getGroupName())
                .groupCount(group.getGroupCount())
                .customRule(group.getCustomRule())
                .challengeType(group.getChallengeType())
                .content(group.getContent())
                .futureDepositRatioRule(group.getFutureDepositRatioRule())
                .durationDays(group.getDurationDays())
                .minCount(group.getMinCount())
                .roundCycleDays(group.getRoundCycleDays())
                .baseDepositAmount(group.getBaseDepositAmount())
                .groupStatus(group.getGroupStatus())
                .build();
    }

    // 그룹 수정
    @Override
    public MessageResponse updateGroup(String userId,
                                       String groupId,
                                       UpdateGroupRequest request) {

        GroupVO group = groupMapper.findGroupById(groupId);

        if (group == null) {
            throw new IllegalArgumentException("존재하지 않는 그룹입니다.");
        }

        if (!group.getUserId().equals(userId)) {
            throw new IllegalArgumentException("그룹 수정 권한이 없습니다.");
        }

        group.setGroupName(request.getGroupName());
        group.setCustomRule(request.getCustomRule());
        group.setContent(request.getContent());
        group.setFutureDepositRatioRule(request.getFutureDepositRatioRule());
        group.setDurationDays(request.getDurationDays());
        group.setMinCount(request.getMinCount());
        group.setRoundCycleDays(request.getRoundCycleDays());
        group.setBaseDepositAmount(request.getBaseDepositAmount());

        groupMapper.updateGroup(group);

        return MessageResponse.builder()
                .message("Success")
                .build();
    }

    @Override
    @Transactional
    public MessageResponse deleteGroup(String userId,
                                       String groupId) {

        GroupVO group = groupMapper.findGroupById(groupId);

        if (group == null) {
            throw new IllegalArgumentException("존재하지 않는 그룹입니다.");
        }

        if (!userId.equals(group.getUserId())) {
            throw new IllegalArgumentException("그룹 종료 권한이 없습니다.");
        }

        // 이미 종료된 그룹인지 확인
        if (group.getGroupStatus() == GroupStatus.FINISHED) {
            throw new IllegalArgumentException("이미 종료된 그룹입니다.");
        }

        groupMapper.finishGroup(groupId);

        return MessageResponse.builder()
                .message("Success")
                .build();
    }
}