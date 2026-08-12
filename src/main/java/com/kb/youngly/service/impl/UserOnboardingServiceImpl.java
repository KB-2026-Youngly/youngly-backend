package com.kb.youngly.service.impl;

import com.kb.youngly.dto.account.AccountRegisterDTO;
import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.user.UserOnboardingRequest;
import com.kb.youngly.mapper.UserMapper;
import com.kb.youngly.service.AccountService;
import com.kb.youngly.service.UserOnboardingService;
import com.kb.youngly.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserOnboardingServiceImpl
        implements UserOnboardingService {

    private final UserMapper userMapper;
    private final AccountService accountService;

    @Override
    @Transactional
    public MessageResponse completeOnboarding(
            String userId,
            UserOnboardingRequest request
    ) {
        UserVO user = userMapper.findByUserId(userId);

        if (user == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 회원입니다."
            );
        }

        validateOnboardingRequest(request);

        /*
         * true와 false는 모두 온보딩을 완료했다는 뜻이다.
         * null인 사용자만 최초 온보딩을 진행할 수 있다.
         */
        if (user.getIsNotificationAgreement() != null) {
            throw new IllegalArgumentException(
                    "이미 초기 설정을 완료한 사용자입니다."
            );
        }

        // 기존 관심 분야를 제거하고 새 선택 항목을 저장한다.
        userMapper.deleteUserInterests(userId);

        userMapper.insertUserInterests(
                userId,
                request.getInterestIds()
        );

        // 선택한 개인 입출금 계좌를 사용자 대표계좌로 연결한다.
        AccountRegisterDTO accountRequest =
                new AccountRegisterDTO();

        accountRequest.setKbAccountId(
                request.getKbAccountId()
        );

        accountService.register(
                accountRequest,
                userId
        );

        /*
         * 모든 작업이 성공한 다음 온보딩 완료 상태를 저장한다.
         * false도 알림에 동의하지 않고 온보딩은 완료했다는 뜻이다.
         */
        userMapper.updateNotificationAgreement(
                userId,
                request.getIsNotificationAgreement()
        );

        return MessageResponse.builder()
                .message("Success")
                .build();
    }

    private void validateOnboardingRequest(
            UserOnboardingRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "온보딩 정보가 필요합니다."
            );
        }

        if (request.getInterestIds() == null
                || request.getInterestIds().isEmpty()) {
            throw new IllegalArgumentException(
                    "관심 분야를 한 개 이상 선택해 주세요."
            );
        }

        if (request.getKbAccountId() == null
                || request.getKbAccountId().isBlank()) {
            throw new IllegalArgumentException(
                    "대표 개인 입출금 통장을 선택해 주세요."
            );
        }

        if (request.getIsNotificationAgreement() == null) {
            throw new IllegalArgumentException(
                    "알림 수신 여부가 필요합니다."
            );
        }
        int existingCount = userMapper.countExistingInterestIds(
                request.getInterestIds()
        );

        long requestedCount = request.getInterestIds()
                .stream()
                .distinct()
                .count();

        if (existingCount != requestedCount) {
            throw new IllegalArgumentException(
                    "존재하지 않는 관심 분야가 포함되어 있습니다."
            );
        }
    }

}