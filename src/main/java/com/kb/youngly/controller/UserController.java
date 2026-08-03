package com.kb.youngly.controller;

import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.user.UpdatePasswordRequest;
import com.kb.youngly.dto.user.UpdateUserRequest;
import com.kb.youngly.dto.user.UserResponse;
import com.kb.youngly.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(Authentication authentication) {

        return ResponseEntity.ok(
                userService.getMyInfo(authentication.getName())
        );
    }

    /**
     * 회원정보 수정
     */
    @PutMapping("/me")
    public ResponseEntity<MessageResponse> updateUser(
            Authentication authentication,
            @RequestBody UpdateUserRequest request) {

        return ResponseEntity.ok(
                userService.updateUser(authentication.getName(), request)
        );
    }

    /**
     * 비밀번호 변경
     */
    @PutMapping("/password")
    public ResponseEntity<MessageResponse> updatePassword(
            Authentication authentication,
            @RequestBody UpdatePasswordRequest request) {

        return ResponseEntity.ok(
                userService.updatePassword(authentication.getName(), request)
        );
    }

    /**
     * 회원 탈퇴 (Soft Delete)
     */
    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> deleteUser(
            Authentication authentication) {

        return ResponseEntity.ok(
                userService.deleteUser(authentication.getName())
        );
    }
}