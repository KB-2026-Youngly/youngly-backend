package com.kb.youngly.service;

import com.kb.youngly.dto.account.AccountSearchDTO;
import com.kb.youngly.dto.account.KbAccountDTO;
import com.kb.youngly.dto.moimaccount.MoimAccountDTO;
import com.kb.youngly.dto.moimaccount.MoimAccountNameUpdateRequest;
import com.kb.youngly.dto.moimaccount.MoimAccountRegisterDTO;

import java.util.List;

public interface MoimAccountService {

    // 모임통장 등록
    String register(MoimAccountRegisterDTO dto, String userId);

    // 내 모임통장 목록 조회
    List<MoimAccountDTO> getAccounts(String userId);

    // 내 KB 모임통장 목록 조회
    List<KbAccountDTO> search(AccountSearchDTO dto);

    // 내 연동된 모임통장 비활성화
    void updateStatus(String moimAccountId);
    // 모임통장 이름 변경
    void updateAccountName(String userId, String moimAccountId, MoimAccountNameUpdateRequest request);
}