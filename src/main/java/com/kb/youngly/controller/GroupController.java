package com.kb.youngly.controller;

import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.group.*;
import com.kb.youngly.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // 그룹 생성
    @PostMapping
    public ResponseEntity<CreateGroupResponse> createGroup(
            Authentication authentication,
            @RequestBody CreateGroupRequest request) {

        String userId = authentication.getName();
        CreateGroupResponse response =
                groupService.createGroup(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 그룹 목록 조회
    @GetMapping
    public ResponseEntity<List<GroupListResponse>> getGroupList(
            Authentication authentication) {

        return ResponseEntity.ok(
                groupService.getGroupList(authentication.getName())
        );
    }

    // 그룹 상세 조회
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailResponse> getGroupDetail(
            Authentication authentication,
            @PathVariable String groupId) {

        return ResponseEntity.ok(
                groupService.getGroupDetail(
                        authentication.getName(),
                        groupId
                )
        );
    }

    // 그룹 수정
    @PutMapping("/{groupId}")
    public ResponseEntity<MessageResponse> updateGroup(
            Authentication authentication,
            @PathVariable String groupId,
            @RequestBody UpdateGroupRequest request) {

        return ResponseEntity.ok(
                groupService.updateGroup(
                        authentication.getName(),
                        groupId,
                        request
                )
        );
    }

    // 그룹 종료(삭제)
    @DeleteMapping("/{groupId}")
    public ResponseEntity<MessageResponse> deleteGroup(
            Authentication authentication,
            @PathVariable String groupId) {

        return ResponseEntity.ok(
                groupService.deleteGroup(
                        authentication.getName(),
                        groupId
                )
        );
    }

    // 그룹 참여
    @PostMapping("/join")
    public ResponseEntity<MessageResponse> joinGroup(
            Authentication authentication,
            @RequestBody JoinGroupRequest request) {

        return ResponseEntity.ok(
                groupService.joinGroup(
                        authentication.getName(),
                        request
                )
        );
    }

    // 승인 대기 목록 조회
    @GetMapping("/{groupId}/join-requests")
    public ResponseEntity<List<JoinRequestResponse>> getJoinRequests(
            Authentication authentication,
            @PathVariable String groupId) {

        return ResponseEntity.ok(
                groupService.getJoinRequests(
                        authentication.getName(),
                        groupId
                )
        );
    }

    // 그룹 참여 승인
    @PutMapping("/{groupId}/join-requests/{groupUserId}/approve")
    public ResponseEntity<MessageResponse> approveJoinRequest(
            Authentication authentication,
            @PathVariable String groupId,
            @PathVariable Long groupUserId) {

        return ResponseEntity.ok(
                groupService.approveJoinRequest(
                        authentication.getName(),
                        groupId,
                        groupUserId
                )
        );
    }

    //그룸 참여 거절
    @PutMapping("/{groupId}/join-requests/{groupUserId}/reject")
    public ResponseEntity<MessageResponse> rejectJoinRequest(
            Authentication authentication,
            @PathVariable String groupId,
            @PathVariable Long groupUserId) {

        return ResponseEntity.ok(
                groupService.rejectJoinRequest(
                        authentication.getName(),
                        groupId,
                        groupUserId
                )
        );
    }

    // 그룹 참여자 조회
    @GetMapping("/{groupId}/groupusers")
    public ResponseEntity<List<GroupUserResponse>> getGroupUsers(
            Authentication authentication,
            @PathVariable String groupId) {

        return ResponseEntity.ok(
                groupService.getGroupUsers(
                        authentication.getName(),
                        groupId
                )
        );
    }

    // 참여자 강퇴
    @DeleteMapping("/{groupId}/groupusers/{groupUserId}")
    public ResponseEntity<MessageResponse> kickGroupUser(
            Authentication authentication,
            @PathVariable String groupId,
            @PathVariable Long groupUserId) {

        return ResponseEntity.ok(
                groupService.kickGroupUser(
                        authentication.getName(),
                        groupId,
                        groupUserId
                )
        );
    }

    // 그룹 탈퇴
    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<MessageResponse> leaveGroup(
            Authentication authentication,
            @PathVariable String groupId) {

        return ResponseEntity.ok(
                groupService.leaveGroup(
                        authentication.getName(),
                        groupId
                )
        );
    }

    // 초대 코드 재발급
    @PostMapping("/{groupId}/invite-code/regenerate")
    public ResponseEntity<RegenerateInviteCodeResponse> regenerateInviteCode(
            Authentication authentication,
            @PathVariable String groupId) {

        return ResponseEntity.ok(
                groupService.regenerateInviteCode(
                        authentication.getName(),
                        groupId
                )
        );
    }

    // 챌린지 생성/수정
    @PutMapping("/{groupId}/challenge")
    public ResponseEntity<MessageResponse> updateChallenge(
            Authentication authentication,
            @PathVariable String groupId,
            @RequestBody UpdateChallengeRequest request) {

        return ResponseEntity.ok(
                groupService.updateChallenge(
                        authentication.getName(),
                        groupId,
                        request
                )
        );
    }

    // 챌린지 삭제
    @DeleteMapping("/{groupId}/challenge")
    public ResponseEntity<MessageResponse> deleteChallenge(
            Authentication authentication,
            @PathVariable String groupId) {

        return ResponseEntity.ok(
                groupService.deleteChallenge(
                        authentication.getName(),
                        groupId
                )
        );
    }
}
