package com.kb.youngly.service;

import com.kb.youngly.dto.user.UserResponse;

public interface UserService {

    UserResponse getMyInfo(String userId);
}