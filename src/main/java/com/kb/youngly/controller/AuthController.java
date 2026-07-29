package com.kb.youngly.controller;

import com.kb.youngly.dto.auth.SignupResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kb.youngly.dto.auth.SignupRequest;
import com.kb.youngly.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @RequestBody SignupRequest signupRequest) {

        SignupResponse response = authService.signup(signupRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}