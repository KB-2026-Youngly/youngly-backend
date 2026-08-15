package com.kb.youngly.controller;

import com.kb.youngly.dto.posts.*;
import com.kb.youngly.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts") // REST API 규격에 맞게 복수형 명사 사용
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;


    /**
     * 인증 게시글 등록
     *
     * POST /api/posts
     * Content-Type: multipart/form-data
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreatePostResponseDTO> createPost(
            Authentication authentication,
            @ModelAttribute CreatePostRequestDTO request) {

        String userId = authentication.getName();

        CreatePostResponseDTO response =
                postService.createPost(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    /**
     * 피드 리스트 API
     * 특정 라운드(그룹)의 날짜별 피드 목록 조회
     * GET /api/posts/groups/{roundId}?date=2026-07-30
     */
    @GetMapping("/groups/{roundId}")
    public ResponseEntity<List<FeedListResponseDTO>> getFeedList(
            @PathVariable Long roundId,
            @RequestParam String date,
            @RequestParam String userId) { // 👈 임시로 쿼리 스트링이나 세션에서 유저 ID를 받아오도록 추가
            // @AuthenticationPrincipal CustomUserDetails userDetails) { // 👈 파라미터 대신 시큐리티가 토큰을 까서 유저 정보를 쥐여줌 (로그인/ 보안 설정 이후 이 코드로 대체)

        // 서비스 호출할 때 userId까지 3개를 넘겨주기
        List<FeedListResponseDTO> feedList = postService.getFeedList(roundId, date, userId);  // userDetails.getUserId()
        return ResponseEntity.ok(feedList);
    }

    /**
     * 피드 상세 정보 API
     * 특정 게시글의 상세 정보 조회 (댓글 + 좋아요/싫어요 유저 목록)
     * GET /api/posts/{postId}/details
     */
    @GetMapping("/{postId}/details")
    public ResponseEntity<FeedDetailResponseDTO> getFeedDetails(@PathVariable Long postId) {

        FeedDetailResponseDTO responseDTO = postService.getFeedDetails(postId);

        return ResponseEntity.ok(responseDTO);
    }

    /**
     * 인증 승인 or 반려 API
     * 특정 게시글을 승인 혹은 반려 판정을 함
     * PATCH /api/posts/{postId}/approval
     */
    @PatchMapping("/{postId}/approval")
    public ResponseEntity<String> processPostApproval(
            @PathVariable Long postId,
            @RequestBody PostApprovalRequestDTO requestDTO) {

        postService.processPostApproval(postId, requestDTO);

        return ResponseEntity.ok("게시글 평가가 성공적으로 반영되었습니다.");
    }

}
