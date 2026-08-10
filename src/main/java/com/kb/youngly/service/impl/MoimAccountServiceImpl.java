package com.kb.youngly.service.impl;

import com.kb.youngly.dto.account.AccountSearchDTO;
import com.kb.youngly.dto.account.KbAccountDTO;
import com.kb.youngly.dto.moimaccount.MoimAccountDTO;
import com.kb.youngly.dto.moimaccount.MoimAccountBalanceSyncResponseDTO;
import com.kb.youngly.dto.moimaccount.MoimAccountRegisterDTO;
import com.kb.youngly.enums.AccountType;
import com.kb.youngly.enums.MoimAccountStatus;
import com.kb.youngly.mapper.KbAccountMapper;
import com.kb.youngly.mapper.MoimAccountMapper;
import com.kb.youngly.service.MoimAccountService;
import com.kb.youngly.util.IdGenerator;
import com.kb.youngly.vo.account.KbAccountVO;
import com.kb.youngly.vo.moimaccount.MoimAccountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MoimAccountServiceImpl implements MoimAccountService {

    private final MoimAccountMapper moimAccountMapper;
    private final KbAccountMapper kbAccountMapper;
    @Override
    public String register(MoimAccountRegisterDTO dto,
                           String userId) {

        KbAccountVO kbAccount =
                getKbAccount(dto.getKbAccountId());

        validateMoimAccount(kbAccount.getAccountType());

        MoimAccountVO exist =
                moimAccountMapper.findByKbAccountId(dto.getKbAccountId());

        if (exist == null) {

            MoimAccountVO account =
                    createAccount(dto, userId, kbAccount);

            moimAccountMapper.insert(account);

            return account.getMoimAccountId();
        }

        if (exist.getAccountStatus() == MoimAccountStatus.DEACTIVATED) {

            exist.setAccountStatus(MoimAccountStatus.ACTIVE);

            moimAccountMapper.updateStatus(exist);

            return exist.getMoimAccountId();
        }

        throw new IllegalArgumentException("이미 등록된 모임통장입니다.");
    }
    @Override
    public List<MoimAccountDTO> getAccounts(String userId) {

        List<MoimAccountDTO> accounts =
                moimAccountMapper.findByUserId(userId);

        if (accounts.isEmpty()) {
            throw new IllegalArgumentException(
                    "등록된 모임통장이 없습니다."
            );
        }

        return accounts;
    }

    @Override
    public void updateStatus(String moimAccountId) {

        MoimAccountVO account =
                getCurrentAccount(moimAccountId);

        account.setAccountStatus(MoimAccountStatus.DEACTIVATED);

        moimAccountMapper.updateStatus(account);
    }

    @Override
    @Transactional
    public MoimAccountBalanceSyncResponseDTO syncBalance(
            String moimAccountId,
            String userId) {

        // 모임통장 존재 여부와 최초 등록 소유자를 먼저 확인한다.
        MoimAccountVO account = getCurrentAccount(moimAccountId);

        if (!account.getUserId().equals(userId)) {
            throw new AccessDeniedException(
                    "본인이 등록한 모임통장만 갱신할 수 있습니다."
            );
        }

        // 소유자 조건을 UPDATE에도 포함해 synced_at을 현재 시각으로 갱신한다.
        moimAccountMapper.updateSyncedAt(moimAccountId, userId);

        // 동일 트랜잭션에서 KB 원장의 최신 잔액과 갱신 시각을 조회한다.
        MoimAccountBalanceSyncResponseDTO result =
                moimAccountMapper.findBalanceSyncResult(
                        moimAccountId,
                        userId
                );

        if (result == null) {
            throw new IllegalArgumentException(
                    "등록된 모임통장이 없습니다."
            );
        }

        return result;
    }

    private KbAccountVO getKbAccount(String kbAccountId) {

        KbAccountVO kbAccount =
                kbAccountMapper.findById(kbAccountId);

        if (kbAccount == null) {
            throw new IllegalArgumentException("존재하지 않는 KB 계좌입니다.");
        }

        return kbAccount;
    }
    @Override
    public List<KbAccountDTO> search(AccountSearchDTO dto) {

        return moimAccountMapper.search(dto);
    }
    private MoimAccountVO createAccount(MoimAccountRegisterDTO dto,
                                        String userId,
                                        KbAccountVO kbAccount) {

        return MoimAccountVO.builder()
                .moimAccountId(IdGenerator.generateMoimAccountId())
                .userId(userId)
                .kbAccountId(dto.getKbAccountId())
                .accountStatus(MoimAccountStatus.ACTIVE)
                .accountName(
                        kbAccount.getBankName()
                                + " "
                                + kbAccount.getAccountNumber()
                )
                .build();
    }
    private void validateMoimAccount(AccountType accountType) {

        if (accountType != AccountType.MOIM) {
            throw new IllegalArgumentException(
                    "모임통장만 등록할 수 있습니다."
            );
        }
    }
    private MoimAccountVO getCurrentAccount(String moimAccountId) {

        MoimAccountVO account =
                moimAccountMapper.findById(moimAccountId);

        if (account == null) {
            throw new IllegalArgumentException(
                    "등록된 모임통장이 없습니다."
            );
        }

        return account;
    }

}
