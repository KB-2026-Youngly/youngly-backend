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

@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @PostMapping("/draw")
    public ResponseEntity<CharacterDrawResponse> drawCharacter(Authentication authentication) {
        return ResponseEntity.ok(
                characterService.drawCharacter(getAuthenticatedUserId(authentication))
        );
    }

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
