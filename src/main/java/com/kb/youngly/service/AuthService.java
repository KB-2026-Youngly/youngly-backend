package com.kb.youngly.service;

import com.kb.youngly.dto.auth.SignupRequest;
import com.kb.youngly.dto.auth.SignupResponse;

public interface AuthService {

    SignupResponse signup(SignupRequest signupRequest);

}