package com.kb.youngly.controller;

import com.kb.youngly.dto.round.RoundResponse;
import com.kb.youngly.dto.round.RoundUserResponse;
import com.kb.youngly.service.RoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rounds")
@RequiredArgsConstructor
public class RoundQueryController {

    private final RoundService roundService;

    // 라운드 조회
    @GetMapping("/{roundId}")
    public ResponseEntity<RoundResponse> getRound(
            @PathVariable Long roundId) {

        return ResponseEntity.ok(
                roundService.getRound(roundId)
        );
    }

    // 라운드 참여자 조회
    @GetMapping("/{roundId}/users")
    public ResponseEntity<List<RoundUserResponse>> getRoundUsers(
            @PathVariable Long roundId) {

        return ResponseEntity.ok(
                roundService.getRoundUsers(roundId)
        );
    }
}