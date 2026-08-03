package com.kb.youngly.controller;

import com.kb.youngly.dto.character.CharacterDrawResponse;
import com.kb.youngly.dto.character.OwnedCharacterResponse;
import com.kb.youngly.service.CharacterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 인증된 사용자의 캐릭터 랜덤 획득과 보유 캐릭터 조회 HTTP API를 제공한다.
 * 요청의 인증 정보에 담긴 사용자 ID를 서비스 계층에 전달한다.
 */
@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    /**
     * 요청 본문 없이 인증된 사용자의 캐릭터 뽑기를 처리하고
     * 획득한 캐릭터 정보와 변경 후 포인트 잔액을 반환한다.
     */
    @PostMapping("/draw")
    public ResponseEntity<CharacterDrawResponse> drawCharacter(Authentication authentication) {
        return ResponseEntity.ok(
                characterService.drawCharacter(getAuthenticatedUserId(authentication))
        );
    }

    /**
     * 인증된 사용자가 보유한 캐릭터 목록을 최근 획득순으로 반환한다.
     */
    @GetMapping("/owned")
    public ResponseEntity<List<OwnedCharacterResponse>> getOwnedCharacters(
            Authentication authentication) {
        return ResponseEntity.ok(
                characterService.getOwnedCharacters(getAuthenticatedUserId(authentication))
        );
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("인증된 사용자 정보가 없습니다.");
        }
        return authentication.getName();
    }
}
