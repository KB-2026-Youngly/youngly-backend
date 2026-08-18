package com.kb.youngly.controller;

import com.kb.youngly.dto.posts.*;
import com.kb.youngly.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
            Authentication authentication,
            @PathVariable Long roundId,
            @RequestParam String date
    ) {
        String userId = authentication.getName();

        return ResponseEntity.ok(
                postService.getFeedList(roundId, date, userId)
        );
    }

    /**
     * 로그인 사용자의 인증 캘린더용 기간 조회입니다. 결과는 postedAt 오름차순입니다.
     * GET /api/posts/mine?from=2026-08-01&to=2026-08-31&groupId=group-id
     */
    @GetMapping("/mine")
    public ResponseEntity<List<MyPostCalendarResponseDTO>> getMyPosts(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String groupId
    ) {
        return ResponseEntity.ok(postService.getMyPostsByPeriod(
                authentication.getName(), from, to, groupId));
    }

    /**
     * 피드 상세 정보 API
     * 특정 게시글의 상세 정보 조회 (댓글 + 좋아요/싫어요 유저 목록)
     * GET /api/posts/{postId}/details
     */
    @GetMapping("/{postId}/details")
    public ResponseEntity<FeedDetailResponseDTO> getFeedDetails(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        String userId = authentication.getName();

        FeedDetailResponseDTO responseDTO =
                postService.getFeedDetails(postId, userId);

        return ResponseEntity.ok(responseDTO);
    }

    /**
     * 인증 승인 or 반려 API
     * 특정 게시글을 승인 혹은 반려 판정을 함
     * PATCH /api/posts/{postId}/approval
     */
    @PatchMapping("/{postId}/approval")
    public ResponseEntity<String> processPostApproval(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestBody PostApprovalRequestDTO requestDTO) {

        String userId = authentication.getName();

        postService.processPostApproval(
                postId,
                userId,
                requestDTO
        );

        return ResponseEntity.ok(
                "게시글 평가가 성공적으로 반영되었습니다."
        );
    }

    /**
     * 댓글 등록
     * POST /api/posts/{postId}/comments
     */
    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentDTO> createComment(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestBody CreateCommentRequestDTO request
    ) {
        String userId = authentication.getName();

        CommentDTO response =
                postService.createComment(postId, userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * 좋아요 또는 싫어요 등록/변경
     * PUT /api/posts/{postId}/reaction
     */
    @PutMapping("/{postId}/reaction")
    public ResponseEntity<PostReactionResponseDTO> setReaction(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestBody PostReactionRequestDTO request
    ) {
        String userId = authentication.getName();

        return ResponseEntity.ok(
                postService.setReaction(postId, userId, request)
        );
    }

    /**
     * 좋아요 또는 싫어요 취소
     * DELETE /api/posts/{postId}/reaction
     */
    @DeleteMapping("/{postId}/reaction")
    public ResponseEntity<PostReactionResponseDTO> deleteReaction(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        String userId = authentication.getName();

        return ResponseEntity.ok(
                postService.deleteReaction(postId, userId)
        );
    }

    /**
     * 본인 인증 게시글 삭제
     * DELETE /api/posts/{postId}
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        String userId = authentication.getName();

        postService.deletePost(postId, userId);

        return ResponseEntity.noContent().build();
    }

}
