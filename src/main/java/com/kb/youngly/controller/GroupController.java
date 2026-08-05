package com.kb.youngly.controller;

import com.kb.youngly.dto.group.CreateGroupRequest;
import com.kb.youngly.dto.group.CreateGroupResponse;
import com.kb.youngly.dto.group.GroupDetailResponse;
import com.kb.youngly.dto.group.GroupListResponse;
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

    @PostMapping
    public ResponseEntity<CreateGroupResponse> createGroup(
            Authentication authentication,
            @RequestBody CreateGroupRequest request) {

        CreateGroupResponse response =
                groupService.createGroup(authentication.getName(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<GroupListResponse>> getGroupList(
            Authentication authentication) {

        return ResponseEntity.ok(
                groupService.getGroupList(authentication.getName())
        );
    }

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
}