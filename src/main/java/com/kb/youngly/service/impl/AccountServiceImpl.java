package com.kb.youngly.service.impl;

import com.kb.youngly.dto.account.*;
import com.kb.youngly.enums.AccountStatus;
import com.kb.youngly.enums.AccountType;
import com.kb.youngly.mapper.AccountMapper;
import com.kb.youngly.mapper.KbAccountMapper;
import com.kb.youngly.service.AccountService;
import com.kb.youngly.util.IdGenerator;
import com.kb.youngly.vo.account.AccountDetailVO;
import com.kb.youngly.vo.account.AccountVO;
import com.kb.youngly.vo.account.KbAccountVO;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final KbAccountMapper kbAccountMapper;

    @Override
    public List<KbAccountDTO> search(AccountSearchDTO dto) {
        List<KbAccountVO> kbAccounts =
                kbAccountMapper.findByNameAndBirthday(dto);

        List<KbAccountDTO> result = new ArrayList<>();

        for (KbAccountVO account : kbAccounts) {

            KbAccountDTO accountDTO = KbAccountDTO.builder()
                    .kbAccountId(account.getKbAccountId())
                    .accountType(account.getAccountType())
                    .accountNumber(account.getAccountNumber())
                    .bankName(account.getBankName())
                    .balance(account.getBalance())
                    .build();

            result.add(accountDTO);
        }

        return result;
    }

    @Override
    public String register(AccountRegisterDTO dto, String userId) {

        KbAccountVO kbAccount = getKbAccount(dto.getKbAccountId());

        validateDuplicateAccount(userId, kbAccount.getAccountType());

        AccountVO account = createAccount(dto, userId, kbAccount);

        accountMapper.insert(account);

        syncDepositAccountStatus(userId);

        return account.getAccountId();
    }

    @Override
    public AccountDTO getAccount(String userId, AccountType accountType) {

        AccountDTO account =
                accountMapper.findByUserIdAndType(userId, accountType);

        if (account == null) {
            throw new IllegalArgumentException("등록된 계좌가 없습니다.");
        }

        return account;
    }

    @Override
    public void update(String accountId,
                       AccountUpdateDTO dto) {

        KbAccountVO kbAccount =
                getKbAccount(dto.getKbAccountId());

        AccountDetailVO currentAccount =
                getCurrentAccount(accountId);

        validateAccountType(
                currentAccount.getAccountType(),
                kbAccount.getAccountType()
        );

        AccountVO account = AccountVO.builder()
                .accountId(accountId)
                .kbAccountId(dto.getKbAccountId())
                .accountName(
                        kbAccount.getBankName()
                                + " "
                                + kbAccount.getAccountNumber()
                )
                .build();

        accountMapper.update(account);

        syncDepositAccountStatus(currentAccount.getUserId());
    }

    @Override
    public void delete(String accountId) {

        // 1. 계좌 조회
        AccountDetailVO account = getCurrentAccount(accountId);

        if (account == null) {
            throw new IllegalArgumentException("등록된 계좌가 없습니다.");
        }

        // 2. 입출금 계좌는 삭제 불가
        if (account.getAccountType() == AccountType.DEPOSIT) {
            throw new IllegalArgumentException("대표 입출금 계좌는 삭제할 수 없습니다.");
        }

        // 3. 삭제
        accountMapper.delete(accountId);
        syncDepositAccountStatus(account.getUserId());
    }
    private KbAccountVO getKbAccount(String kbAccountId) {

        KbAccountVO kbAccount = kbAccountMapper.findById(kbAccountId);

        if (kbAccount == null) {
            throw new IllegalArgumentException("존재하지 않는 KB 계좌입니다.");
        }

        return kbAccount;
    }
    private AccountVO createAccount(AccountRegisterDTO dto,
                                    String userId,
                                    KbAccountVO kbAccount) {

        AccountVO account = AccountVO.builder()
                .kbAccountId(dto.getKbAccountId())
                .build();

        account.setAccountId(IdGenerator.generateAccountId());

        account.setUserId(userId);

        account.setAccountName(
                kbAccount.getBankName() + " " + kbAccount.getAccountNumber()
        );

        if (kbAccount.getAccountType() == AccountType.DEPOSIT) {
            account.setAccountStatus(AccountStatus.INOUTCOME);
        } else if (kbAccount.getAccountType() == AccountType.PENSION) {
            account.setAccountStatus(AccountStatus.NONE);
        }

        return account;

    }
    private void validateDuplicateAccount(String userId,
                                          AccountType accountType) {

        AccountDTO account =
                accountMapper.findByUserIdAndType(userId, accountType);

        if (account != null) {
            throw new IllegalArgumentException(
                    "이미 등록된 대표계좌가 있습니다."
            );
        }
    }
    private AccountDetailVO getCurrentAccount(String accountId) {

        AccountDetailVO account =
                accountMapper.findById(accountId);

        if (account == null) {
            throw new IllegalArgumentException("등록된 계좌가 없습니다.");
        }

        return account;
    }
    private void validateAccountType(AccountType currentType,
                                     AccountType newType){

        if(currentType != newType){
            throw new IllegalArgumentException(
                    "대표계좌 유형은 변경할 수 없습니다."
            );
        }

    }
    private void syncDepositAccountStatus(String userId) {

        AccountDTO deposit =
                accountMapper.findByUserIdAndType(
                        userId,
                        AccountType.DEPOSIT
                );

        if (deposit == null) {
            return;
        }

        AccountDTO pension =
                accountMapper.findByUserIdAndType(
                        userId,
                        AccountType.PENSION
                );

        AccountStatus depositStatus = AccountStatus.INOUTCOME;

        if (pension != null &&
                pension.getAccountStatus() == AccountStatus.INCOME) {

            depositStatus = AccountStatus.OUTCOME;
        }

        AccountVO account = AccountVO.builder()
                .accountId(deposit.getAccountId())
                .accountStatus(depositStatus)
                .build();

        accountMapper.updateAccountStatus(account);
    }
    @Override
    public void updatePensionStatus(String accountId, PensionStatusUpdateDTO dto) {

        AccountDetailVO account =
                getCurrentAccount(accountId);

        validatePensionAccount(account.getAccountType());

        validatePensionStatus(dto.getAccountStatus());

        AccountVO updateAccount = AccountVO.builder()
                .accountId(accountId)
                .accountStatus(dto.getAccountStatus())
                .build();

        accountMapper.updateAccountStatus(updateAccount);

        syncDepositAccountStatus(account.getUserId());
    }
    private void validatePensionAccount(AccountType accountType) {

        if (accountType != AccountType.PENSION) {
            throw new IllegalArgumentException(
                    "개인연금 계좌만 상태를 변경할 수 있습니다."
            );
        }
    }
    private void validatePensionStatus(AccountStatus accountStatus) {

        if (accountStatus != AccountStatus.INCOME &&
                accountStatus != AccountStatus.NONE) {

            throw new IllegalArgumentException(
                    "연금 계좌는 INCOME 또는 NONE만 선택할 수 있습니다."
            );
        }
    }
}