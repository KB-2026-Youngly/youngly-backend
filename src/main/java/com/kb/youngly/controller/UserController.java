package com.kb.youngly.controller;

import com.kb.youngly.dto.user.UserResponse;
import com.kb.youngly.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.user.UpdatePasswordRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(Authentication authentication) {

        return ResponseEntity.ok(
                userService.getMyInfo(authentication.getName())
        );
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponse> updatePassword(
            Authentication authentication,
            @RequestBody UpdatePasswordRequest request) {

        return ResponseEntity.ok(
                userService.updatePassword(authentication.getName(), request)
        );
    }
}