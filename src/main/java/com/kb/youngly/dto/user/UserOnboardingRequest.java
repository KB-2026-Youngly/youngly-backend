package com.kb.youngly.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UserOnboardingRequest {

    private List<Long> interestIds;

    private String kbAccountId;

    private Boolean isNotificationAgreement;
}