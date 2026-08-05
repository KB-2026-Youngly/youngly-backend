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

    /** 인증 연동 전 그룹 생성 API 호출에 사용할 임시 사용자 ID. */
    private static final String DEVELOPMENT_USER_ID = "user01";

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // 그룹 생성
    @PostMapping
    public ResponseEntity<CreateGroupResponse> createGroup(
            Authentication authentication,
            @RequestBody CreateGroupRequest request) {

        CreateGroupResponse response =
                groupService.createGroup(DEVELOPMENT_USER_ID, request);

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
}