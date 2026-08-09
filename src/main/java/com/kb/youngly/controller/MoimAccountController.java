package com.kb.youngly.controller;

import com.kb.youngly.dto.account.AccountSearchDTO;
import com.kb.youngly.dto.account.KbAccountDTO;
import com.kb.youngly.dto.moimaccount.*;
import com.kb.youngly.service.MoimAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group-accounts")
@RequiredArgsConstructor
public class MoimAccountController {

    private final MoimAccountService moimAccountService;

    @PostMapping
    public ResponseEntity<MoimAccountRegisterResponseDTO> register(
            @RequestBody MoimAccountRegisterDTO dto,
            Authentication authentication) {

        String userId = authentication.getName();

        String moimAccountId =
                moimAccountService.register(dto, userId);

        return ResponseEntity.ok(
                new MoimAccountRegisterResponseDTO(
                        moimAccountId,
                        "Success"
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<MoimAccountDTO>> getAccounts(
            Authentication authentication) {

        String userId = authentication.getName();

        return ResponseEntity.ok(
                moimAccountService.getAccounts(userId)
        );
    }


    @PostMapping("/search")
    public ResponseEntity<List<KbAccountDTO>> search(
            @RequestBody AccountSearchDTO dto){

        return ResponseEntity.ok(
                moimAccountService.search(dto)
        );
    }
    @PatchMapping("/{moimAccountId}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable String moimAccountId) {

        moimAccountService.updateStatus(moimAccountId);

        return ResponseEntity.ok(
                Map.of("message", "Success")
        );
    }
    // 모임통장 이름 변경
    @PatchMapping("/{moimAccountId}/name")
    public ResponseEntity<Map<String, String>> updateAccountName(
            @PathVariable String moimAccountId,
            @RequestBody MoimAccountNameUpdateRequest request,
            Authentication authentication
    ) {
        String userId = authentication.getName();

        moimAccountService.updateAccountName(
                userId,
                moimAccountId,
                request
        );

        return ResponseEntity.ok(
                Map.of("message", "Success")
        );
    }
}