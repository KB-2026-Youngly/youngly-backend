package com.kb.youngly.controller;

import com.kb.youngly.dto.round.RoundResponse;
import com.kb.youngly.service.RoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rounds")
@RequiredArgsConstructor
public class RoundQueryController {

    private final RoundService roundService;

    @GetMapping("/{roundId}")
    public ResponseEntity<RoundResponse> getRound(
            @PathVariable Long roundId) {

        return ResponseEntity.ok(
                roundService.getRound(roundId)
        );
    }
}