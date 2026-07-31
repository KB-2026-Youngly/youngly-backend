package com.kb.youngly.controller;

import com.kb.youngly.dto.PostDTO;
import com.kb.youngly.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts") // REST API 규격에 맞게 복수형 명사 사용
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 인증 게시글 등록 (이미지 업로드 포함)
     * POST /api/posts
     */
    @PostMapping
    public ResponseEntity<String> createPost(@ModelAttribute PostDTO postDTO) {
        // 1. 서비스에 비즈니스 로직(파일 저장 + DB INSERT) 위임
        postService.createPost(postDTO);

        // 2. 성공 시 클라이언트에게 깔끔하게 응답 반환
        return ResponseEntity.ok("인증 게시글 및 이미지 업로드 성공!");
    }
}