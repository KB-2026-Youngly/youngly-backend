package com.kb.youngly.service.impl;

import com.kb.youngly.dto.group.CreateGroupRequest;
import com.kb.youngly.dto.group.CreateGroupResponse;
import com.kb.youngly.dto.group.GroupListResponse;
import com.kb.youngly.enums.GroupStatus;
import com.kb.youngly.mapper.GroupMapper;
import com.kb.youngly.service.GroupService;
import com.kb.youngly.vo.group.GroupVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupMapper groupMapper;

    public GroupServiceImpl(GroupMapper groupMapper) {
        this.groupMapper = groupMapper;
    }

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

    @Override
    public List<GroupListResponse> getGroupList(String userId) {

        return groupMapper.findGroupsByUserId(userId);
    }
}