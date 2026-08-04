package com.kb.youngly.dto.posts;

import lombok.Data;
import java.time.LocalDateTime;

// 피드 리스트 화면을 그릴 떄 필요한 요약본
// 게시글 정보, 작성자 정보, 반응 횟수 (좋아요, 싫어요, 댓글 수)
// + 리스트에서 바로 볼 수 있는 '최신 댓글 1개'
@Data
public class FeedListResponseDTO {
    private Long postId;
    private String photoUrl;
    private String content;
    private LocalDateTime postedAt;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;

    // 게시글 승인 상태 & 최신 댓글 1개
    private String postStatus;
    private String latestCommentContent;

    // 작성자 정보
    private String userId;
    private String nickname;
    private String profileImageUrl;
}