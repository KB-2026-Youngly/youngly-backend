package com.kb.youngly.service.impl;

import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.group.*;
import com.kb.youngly.dto.round.RoundResponse;
import com.kb.youngly.enums.GroupStatus;
import com.kb.youngly.enums.GroupUserStatus;
import com.kb.youngly.enums.NotificationType;
import com.kb.youngly.mapper.GroupMapper;
import com.kb.youngly.mapper.GroupUserMapper;
import com.kb.youngly.mapper.RoundMapper;
import com.kb.youngly.mapper.UserMapper;
import com.kb.youngly.service.GroupService;
import com.kb.youngly.service.NotificationService;
import com.kb.youngly.vo.group.GroupUserVO;
import com.kb.youngly.vo.group.GroupVO;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.security.SecureRandom;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupMapper groupMapper;
    private final GroupUserMapper groupUserMapper;
    private final RoundMapper roundMapper;
    private final NotificationService notificationService;

    private static final String INVITE_CODE_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int INVITE_CODE_LENGTH = 6;

    private final SecureRandom secureRandom = new SecureRandom();

    private String generateInviteCode() {

        String inviteCode;

        do {
            StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);

            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                code.append(
                        INVITE_CODE_CHARS.charAt(
                                secureRandom.nextInt(INVITE_CODE_CHARS.length())
                        )
                );
            }

            inviteCode = code.toString();

        } while (groupMapper.existsInviteCode(inviteCode));

        return inviteCode;
    }

    public GroupServiceImpl(GroupMapper groupMapper,
                            GroupUserMapper groupUserMapper,
                            RoundMapper roundMapper,
                            NotificationService notificationService) {

        this.groupMapper = groupMapper;
        this.groupUserMapper = groupUserMapper;
        this.roundMapper = roundMapper;
        this.notificationService = notificationService;
    }

    // 그룹 생성
    @Override
    public CreateGroupResponse createGroup(String userId,
                                           CreateGroupRequest request) {

        GroupVO group = GroupVO.builder()
                .groupId(UUID.randomUUID().toString())
                .moimAccountId(request.getMoimAccountId())
                .userId(userId)
                .inviteCode(generateInviteCode())
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

    /**
     * 로그인 사용자가 조회할 수 있는 그룹인지 확인한 후,
     * 해당 그룹이 진행한 라운드 목록을 최신 회차순으로 반환한다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoundResponse> getGroupRounds(String userId,
                                              String groupId) {

        // 존재하지 않는 그룹의 라운드가 조회되지 않도록 그룹을 먼저 확인한다.
        GroupVO group = groupMapper.findGroupById(groupId);

        if (group == null) {
            throw new IllegalArgumentException("존재하지 않는 그룹입니다.");
        }

        // 그룹장은 별도의 group_users 정보가 없어도 라운드 목록을 조회할 수 있다.
        if (!group.getUserId().equals(userId)) {
            // 그룹장이 아니라면 승인된 참여자인지 확인한다.
            // 예치 전(PENDING_DEPOSIT) 또는 참여 중(ACTIVE)인 사용자만 조회를 허용한다.
            GroupUserVO groupUser = groupUserMapper.findGroupUser(groupId, userId);
            if (groupUser == null ||
                    (groupUser.getGroupUserStatus() != GroupUserStatus.ACTIVE &&
                            groupUser.getGroupUserStatus() != GroupUserStatus.PENDING_DEPOSIT)) {
                throw new AccessDeniedException("라운드 목록 조회 권한이 없습니다.");
            }
        }

        // 그룹의 모든 라운드를 회차 번호 내림차순으로 조회한다.
        return roundMapper.findRoundsByGroupId(groupId);
    }

    // 그룹 수정
    @Override
    public MessageResponse updateGroup(String userId,
                                       String groupId,
                                       UpdateGroupRequest request) {

        GroupVO group = validateLeader(userId, groupId);

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

    // 그룹 종료
    @Override
    @Transactional
    public MessageResponse deleteGroup(String userId,
                                       String groupId) {

        GroupVO group = validateLeader(userId, groupId);

        // 이미 종료된 그룹인지 확인
        if (group.getGroupStatus() == GroupStatus.FINISHED) {
            throw new IllegalArgumentException("이미 종료된 그룹입니다.");
        }

        groupMapper.finishGroup(groupId);

        return MessageResponse.builder()
                .message("Success")
                .build();
    }

    // 그룹 참여
    @Override
    @Transactional
    public MessageResponse joinGroup(String userId,
                                     JoinGroupRequest request) {

        // 초대코드로 그룹 조회
        GroupVO group = groupMapper.findGroupByInviteCode(request.getInviteCode());

        if (group == null) {
            throw new IllegalArgumentException("존재하지 않는 초대코드입니다.");
        }

        // 종료된 그룹 확인
        if (group.getGroupStatus() == GroupStatus.FINISHED) {
            throw new IllegalArgumentException("종료된 그룹입니다.");
        }

        // 이미 참여 여부 확인
        GroupUserVO groupUser =
                groupUserMapper.findGroupUser(group.getGroupId(), userId);

        if (groupUser != null) {

            switch (groupUser.getGroupUserStatus()) {

                case ACTIVE:
                    throw new IllegalArgumentException("이미 참여 중인 그룹입니다.");

                case PENDING_APPROVAL:
                    throw new IllegalArgumentException("이미 가입 신청한 그룹입니다.");

                default:
                    break;
            }
        }

        // 참여 신청
        GroupUserVO newGroupUser = GroupUserVO.builder()
                .groupId(group.getGroupId())
                .userId(userId)
                .groupUserStatus(GroupUserStatus.PENDING_APPROVAL)
                .build();

        groupUserMapper.insertGroupUser(newGroupUser);

        notificationService.createNotification(
                group.getUserId(),  // 그룹장
                NotificationType.APPROVAL_REQUEST,
                "새로운 그룹 참여 신청이 있습니다."
        );

        return MessageResponse.builder()
                .message("Success")
                .build();
    }

    // 승인 대기 목록 조회
    @Override
    public List<JoinRequestResponse> getJoinRequests(
            String userId,
            String groupId) {

        validateLeader(userId, groupId);

        return groupUserMapper.findPendingGroupUsers(groupId);
    }

    // 그룹 참여 승인
    @Override
    @Transactional
    public MessageResponse approveJoinRequest(
            String userId,
            String groupId,
            Long groupUserId) {

        validateLeader(userId, groupId);

        GroupUserVO groupUser =
                validateGroupUser(groupUserId, groupId);

        if (groupUser.getGroupUserStatus() != GroupUserStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("승인 가능한 상태가 아닙니다.");
        }

        groupUserMapper.updateGroupUserStatus(
                groupUserId,
                GroupUserStatus.PENDING_DEPOSIT
        );

        notificationService.createNotification(
                groupUser.getUserId(),
                NotificationType.APPROVED,
                "그룹 참여가 승인되었습니다."
        );

        return MessageResponse.builder()
                .message("Success")
                .build();
    }

    // 그룹 참여 거절
    @Override
    @Transactional
    public MessageResponse rejectJoinRequest(
            String userId,
            String groupId,
            Long groupUserId) {

        validateLeader(userId, groupId);

        GroupUserVO groupUser =
                validateGroupUser(groupUserId, groupId);

        if (groupUser.getGroupUserStatus() != GroupUserStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("거절 가능한 상태가 아닙니다.");
        }

        groupUserMapper.updateGroupUserStatus(
                groupUserId,
                GroupUserStatus.REJECTED
        );

        notificationService.createNotification(
                groupUser.getUserId(),
                NotificationType.REJECTED,
                "그룹 참여가 거절되었습니다."
        );

        return MessageResponse.builder()
                .message("Success")
                .build();
    }

    /**
     * 그룹 존재 여부 및 총무 권한 검증
     */
    private GroupVO validateLeader(String userId, String groupId) {

        GroupVO group = groupMapper.findGroupById(groupId);

        if (group == null) {
            throw new IllegalArgumentException("존재하지 않는 그룹입니다.");
        }

        if (!group.getUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        return group;
    }

    /**
     * 참여 신청 존재 여부 및 그룹 일치 여부 검증
     */
    private GroupUserVO validateGroupUser(Long groupUserId,
                                          String groupId) {

        GroupUserVO groupUser =
                groupUserMapper.findGroupUserById(groupUserId);

        if (groupUser == null) {
            throw new IllegalArgumentException("존재하지 않는 참여 신청입니다.");
        }

        if (!groupUser.getGroupId().equals(groupId)) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        return groupUser;
    }

    // 그룹 참여자 조회
    @Override
    public List<GroupUserResponse> getGroupUsers(
            String userId,
            String groupId) {

        // 그룹 존재 확인
        GroupVO group = groupMapper.findGroupById(groupId);

        if (group == null) {
            throw new IllegalArgumentException("존재하지 않는 그룹입니다.");
        }

        // 로그인한 사용자가 그룹 참여자인지 확인
        GroupUserVO groupUser =
                groupUserMapper.findGroupUser(groupId, userId);

        if (groupUser == null ||
                groupUser.getGroupUserStatus() != GroupUserStatus.ACTIVE) {

            throw new IllegalArgumentException("조회 권한이 없습니다.");
        }

        // 참여자 목록 조회
        return groupUserMapper.findGroupUsers(groupId);
    }

    // 참여자 강퇴
    @Override
    @Transactional
    public MessageResponse kickGroupUser(
            String userId,
            String groupId,
            Long groupUserId) {

        // 총무 권한 검증
        GroupVO group = validateLeader(userId, groupId);

        // 강퇴 대상 조회
        GroupUserVO target = validateGroupUser(groupUserId, groupId);

        // ACTIVE, PENDING_DEPOSIT 상태만 강퇴 가능
        if (target.getGroupUserStatus() != GroupUserStatus.ACTIVE &&
                target.getGroupUserStatus() != GroupUserStatus.PENDING_DEPOSIT) {
            throw new IllegalArgumentException("강퇴 가능한 상태가 아닙니다.");
        }

        // 총무 자기 자신 강퇴 방지
        if (target.getUserId().equals(group.getUserId())) {
            throw new IllegalArgumentException("총무는 자신을 강퇴할 수 없습니다.");
        }

        // 상태 변경
        groupUserMapper.updateGroupUserStatus(
                groupUserId,
                GroupUserStatus.WITHDRAWN
        );

        return MessageResponse.builder()
                .message("Success")
                .build();
    }

    // 그룹 탈퇴
    @Override
    @Transactional
    public MessageResponse leaveGroup(
            String userId,
            String groupId) {

        // 그룹 존재 확인
        GroupVO group = groupMapper.findGroupById(groupId);

        if (group == null) {
            throw new IllegalArgumentException("존재하지 않는 그룹입니다.");
        }

        // 총무는 탈퇴 불가
        if (group.getUserId().equals(userId)) {
            throw new IllegalArgumentException("총무는 그룹을 탈퇴할 수 없습니다.");
        }

        // 내 참여 정보 조회
        GroupUserVO groupUser =
                groupUserMapper.findGroupUser(groupId, userId);

        if (groupUser == null) {
            throw new IllegalArgumentException("그룹 참여자가 아닙니다.");
        }

        // ACTIVE, PENDING_DEPOSIT 상태만 탈퇴 가능
        if (groupUser.getGroupUserStatus() != GroupUserStatus.ACTIVE &&
                groupUser.getGroupUserStatus() != GroupUserStatus.PENDING_DEPOSIT) {
            throw new IllegalArgumentException("탈퇴 가능한 상태가 아닙니다.");
        }

        // 탈퇴 처리
        groupUserMapper.updateGroupUserStatus(
                groupUser.getGroupUserId(),
                GroupUserStatus.WITHDRAWN
        );

        // 그룹 총무에게 탈퇴 알림
        notificationService.createNotification(
                group.getUserId(),
                NotificationType.LEFT,
                userId + "님이 그룹에서 탈퇴했습니다."
        );

        return MessageResponse.builder()
                .message("Success")
                .build();
    }

    // 초대 코드 재발급
    @Override
    @Transactional
    public RegenerateInviteCodeResponse regenerateInviteCode(
            String userId,
            String groupId) {

        // 총무 권한 검증
        validateLeader(userId, groupId);

        // 중복되지 않는 6자리 초대코드 생성
        String inviteCode = generateInviteCode();

        // DB 업데이트
        groupMapper.updateInviteCode(groupId, inviteCode);

        // 응답
        return RegenerateInviteCodeResponse.builder()
                .inviteCode(inviteCode)
                .build();
    }

    // 챌린지 생성/수정
    @Override
    @Transactional
    public MessageResponse updateChallenge(
            String userId,
            String groupId,
            UpdateChallengeRequest request) {

        GroupVO group = validateLeader(userId, groupId);

        validateChallenge(request);

        group.setChallengeType(request.getChallengeType());
        group.setContent(request.getContent());
        group.setFutureDepositRatioRule(request.getFutureDepositRatioRule());
        group.setDurationDays(request.getDurationDays());
        group.setMinCount(request.getMinCount());
        group.setRoundCycleDays(request.getRoundCycleDays());
        group.setBaseDepositAmount(request.getBaseDepositAmount());

        groupMapper.updateChallenge(group);

        return MessageResponse.builder()
                .message("Success")
                .build();
    }

    private void validateChallenge(UpdateChallengeRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("챌린지 정보는 필수입니다.");
        }

        if (request.getChallengeType() == null) {
            throw new IllegalArgumentException("챌린지 유형은 필수입니다.");
        }

        if (request.getDurationDays() == null ||
                request.getDurationDays() <= 0) {
            throw new IllegalArgumentException("챌린지 기간은 1일 이상이어야 합니다.");
        }

        if (request.getMinCount() == null ||
                request.getMinCount() <= 0) {
            throw new IllegalArgumentException("최소 인증 횟수는 1 이상이어야 합니다.");
        }

        if (request.getRoundCycleDays() == null ||
                request.getRoundCycleDays() <= 0) {
            throw new IllegalArgumentException("라운드 주기는 1일 이상이어야 합니다.");
        }

        if (request.getBaseDepositAmount() == null ||
                request.getBaseDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("기본 예치금은 0보다 커야 합니다.");
        }
    }

    // 챌린지 삭제
    @Override
    @Transactional
    public MessageResponse deleteChallenge(
            String userId,
            String groupId) {

        validateLeader(userId, groupId);

        groupMapper.deleteChallenge(groupId);

        return MessageResponse.builder()
                .message("Success")
                .build();
    }
}
