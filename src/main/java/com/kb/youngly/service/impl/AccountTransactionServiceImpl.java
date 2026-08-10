package com.kb.youngly.service.impl;

import com.kb.youngly.dto.account.AccountTransactionResponse;
import com.kb.youngly.enums.AccountType;
import com.kb.youngly.mapper.AccountTransactionMapper;
import com.kb.youngly.service.AccountTransactionService;
import com.kb.youngly.vo.account.AccountTransactionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountTransactionServiceImpl
        implements AccountTransactionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AccountTransactionMapper accountTransactionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AccountTransactionResponse> getTransactions(
            String userId,
            AccountType accountType,
            String accountId,
            Long roundId,
            int page,
            int size
    ) {
        validateRequest(userId, accountType, accountId, page, size);

        String kbAccountId = resolveAccessibleKbAccountId(
                userId,
                accountType,
                accountId
        );

        if (kbAccountId == null) {
            throw new AccessDeniedException(
                    "해당 계좌의 거래내역을 조회할 권한이 없습니다."
            );
        }

        int offset = Math.multiplyExact(page, size);

        return accountTransactionMapper
                .findByKbAccountId(kbAccountId, roundId, size, offset)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String resolveAccessibleKbAccountId(
            String userId,
            AccountType accountType,
            String accountId
    ) {
        if (accountType == AccountType.MOIM) {
            return accountTransactionMapper.findMoimKbAccountId(
                    userId,
                    accountId
            );
        }

        if (accountType == AccountType.DEPOSIT
                || accountType == AccountType.PENSION) {
            return accountTransactionMapper.findPersonalKbAccountId(
                    userId,
                    accountId,
                    accountType
            );
        }

        throw new IllegalArgumentException(
                "지원하지 않는 계좌 유형입니다."
        );
    }

    private void validateRequest(
            String userId,
            AccountType accountType,
            String accountId,
            int page,
            int size
    ) {
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        if (accountType == null) {
            throw new IllegalArgumentException(
                    "계좌 유형은 필수입니다."
            );
        }

        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException(
                    "계좌 ID는 필수입니다."
            );
        }

        if (page < 0) {
            throw new IllegalArgumentException(
                    "페이지 번호는 0 이상이어야 합니다."
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "페이지 크기는 1~100 사이여야 합니다."
            );
        }
    }

    private AccountTransactionResponse toResponse(
            AccountTransactionVO transaction
    ) {
        return AccountTransactionResponse.builder()
                .transactionId(
                        transaction.getAccountTransactionId()
                )
                .transactionType(
                        transaction.getTransactionType()
                )
                .transactionCategory(
                        transaction.getTransactionCategory()
                )
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .roundId(transaction.getRoundId())
                .roundNo(transaction.getRoundNo())
                .anotherAccountNumber(
                        transaction.getAnotherAccountNumber()
                )
                .anotherBankName(
                        transaction.getAnotherBankName()
                )
                .anotherName(transaction.getAnotherName())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}