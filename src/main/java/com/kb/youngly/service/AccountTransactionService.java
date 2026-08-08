package com.kb.youngly.service;

import com.kb.youngly.dto.account.AccountTransactionResponse;
import com.kb.youngly.enums.AccountType;

import java.util.List;

public interface AccountTransactionService {

    List<AccountTransactionResponse> getTransactions(
            String userId,
            AccountType accountType,
            String accountId,
            int page,
            int size
    );
}