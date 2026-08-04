package com.kb.youngly.controller;

import com.kb.youngly.dto.group.CreateGroupRequest;
import com.kb.youngly.dto.group.CreateGroupResponse;
import com.kb.youngly.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    /** 인증 연동 전 그룹 생성 API 호출에 사용할 임시 사용자 ID. */
    private static final String DEVELOPMENT_USER_ID = "user01";

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<CreateGroupResponse> createGroup(
            @RequestBody CreateGroupRequest request) {

        CreateGroupResponse response =
                groupService.createGroup(DEVELOPMENT_USER_ID, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
