package com.kb.youngly.controller;

import com.kb.youngly.dto.account.AccountTransactionResponse;
import com.kb.youngly.enums.AccountType;
import com.kb.youngly.service.AccountTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account-transactions")
public class AccountTransactionController {

    private final AccountTransactionService accountTransactionService;

    @GetMapping
    public ResponseEntity<List<AccountTransactionResponse>>
    getTransactions(
            Authentication authentication,
            @RequestParam AccountType accountType,
            @RequestParam String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String userId = authentication.getName();

        return ResponseEntity.ok(
                accountTransactionService.getTransactions(
                        userId,
                        accountType,
                        accountId,
                        page,
                        size
                )
        );
    }
}