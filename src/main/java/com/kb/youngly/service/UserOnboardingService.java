package com.kb.youngly.service;

import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.user.UserOnboardingRequest;

public interface UserOnboardingService {

    MessageResponse completeOnboarding(
            String userId,
            UserOnboardingRequest request
    );
}