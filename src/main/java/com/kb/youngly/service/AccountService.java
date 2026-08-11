package com.kb.youngly.service;

import com.kb.youngly.dto.account.*;
import com.kb.youngly.enums.AccountType;

import java.util.List;

public interface AccountService {

    // KB 계좌 조회
    List<KbAccountDTO> search(AccountSearchDTO dto);

    // 계좌 등록
    String register(AccountRegisterDTO dto, String userId);

    // 대표 계좌 조회
    AccountDTO getAccount(String userId, AccountType accountType);

    // 계좌 변경
    void update(String accountId, AccountUpdateDTO dto);

    // 개인연금 사용 상태 변경
    void updatePensionStatus(String accountId, PensionStatusUpdateDTO dto);

    // 계좌 삭제
    void delete(String accountId);

    // KB 원장 최신 잔액 조회 및 동기화 시각 갱신
    AccountBalanceSyncResponseDTO syncBalance(String accountId, String userId);
}
