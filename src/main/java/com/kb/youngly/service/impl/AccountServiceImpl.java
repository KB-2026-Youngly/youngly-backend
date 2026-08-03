package com.kb.youngly.service.impl;

import com.kb.youngly.dto.account.*;
import com.kb.youngly.enums.AccountStatus;
import com.kb.youngly.enums.AccountType;
import com.kb.youngly.mapper.AccountMapper;
import com.kb.youngly.mapper.KbAccountMapper;
import com.kb.youngly.service.AccountService;
import com.kb.youngly.util.IdGenerator;
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

        validateAccountStatus(
                kbAccount.getAccountType(),
                dto.getAccountStatus()
        );

        AccountVO account = createAccount(dto, userId, kbAccount);

        accountMapper.insert(account);

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
        AccountDTO currentAccount =
                getCurrentAccount(accountId);

        validateAccountType(
                currentAccount.getAccountType(),
                kbAccount.getAccountType()
        );

        validateAccountStatus(
                kbAccount.getAccountType(),
                dto.getAccountStatus()
        );

        AccountVO account = AccountVO.builder()
                .accountId(accountId)
                .kbAccountId(dto.getKbAccountId())
                .accountStatus(dto.getAccountStatus())
                .accountName(
                        kbAccount.getBankName()
                                + " "
                                + kbAccount.getAccountNumber()
                )
                .build();

        accountMapper.update(account);
    }

    @Override
    public void delete(String accountId) {

        // 1. 계좌 조회
        AccountDTO account = accountMapper.findByAccountId(accountId);

        if (account == null) {
            throw new IllegalArgumentException("등록된 계좌가 없습니다.");
        }

        // 2. 입출금 계좌는 삭제 불가
        if (account.getAccountType() == AccountType.DEPOSIT) {
            throw new IllegalArgumentException("대표 입출금 계좌는 삭제할 수 없습니다.");
        }

        // 3. 삭제
        accountMapper.delete(accountId);
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

        AccountVO account = dto.toVO();

        account.setAccountId(IdGenerator.generateAccountId());

        account.setUserId(userId);

        account.setAccountName(
                kbAccount.getBankName() + " " + kbAccount.getAccountNumber()
        );

        return account;
    }

    private void validateAccountStatus(AccountType accountType,
                                       AccountStatus accountStatus) {

        switch (accountType) {

            case DEPOSIT:
                if (accountStatus != AccountStatus.OUTCOME &&
                        accountStatus != AccountStatus.INOUTCOME) {

                    throw new IllegalArgumentException("입출금 계좌의 상태가 올바르지 않습니다.");
                }
                break;

            case PENSION:
                if (accountStatus != AccountStatus.NONE &&
                        accountStatus != AccountStatus.INCOME) {

                    throw new IllegalArgumentException("연금 계좌의 상태가 올바르지 않습니다.");
                }
                break;

            default:
                throw new IllegalArgumentException("등록할 수 없는 계좌입니다.");
        }
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
    private AccountDTO getCurrentAccount(String accountId){

        AccountDTO account =
                accountMapper.findByAccountId(accountId);

        if(account == null){
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
}