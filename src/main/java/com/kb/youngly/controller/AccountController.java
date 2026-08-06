package com.kb.youngly.controller;

import com.kb.youngly.dto.account.*;
import com.kb.youngly.enums.AccountType;
import com.kb.youngly.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/search")
    public ResponseEntity<List<KbAccountDTO>> search(
            @RequestBody AccountSearchDTO dto) {

        return ResponseEntity.ok(accountService.search(dto));

    }
    @PostMapping
    public ResponseEntity<AccountRegisterResponseDTO> register(
            @RequestBody AccountRegisterDTO dto,
            Authentication authentication) {

        String userId = authentication.getName();

        String accountId = accountService.register(dto, userId);

        return ResponseEntity.ok(
                new AccountRegisterResponseDTO(
                        accountId,
                        "Success"
                )
        );
    }
    @GetMapping
    public ResponseEntity<AccountDTO> getAccount(
            @RequestParam AccountType accountType,
            Authentication authentication)  {
        String userId = authentication.getName();

        return ResponseEntity.ok(
                accountService.getAccount(userId, accountType)
        );
    }
    @PutMapping("/{accountId}")
    public ResponseEntity<Map<String,String>> update(
            @PathVariable String accountId,
            @RequestBody AccountUpdateDTO dto){

        accountService.update(accountId,dto);

        return ResponseEntity.ok(
                Map.of("message","Success")
        );
    }
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable String accountId) {

        accountService.delete(accountId);

        return ResponseEntity.ok(
                Map.of("message", "Success")
        );
    }
    @PatchMapping("/{accountId}/status")
    public ResponseEntity<Map<String, String>> updatePensionStatus(
            @PathVariable String accountId,
            @RequestBody PensionStatusUpdateDTO dto) {

        accountService.updatePensionStatus(accountId, dto);

        return ResponseEntity.ok(
                Map.of("message", "Success")
        );
    }
}