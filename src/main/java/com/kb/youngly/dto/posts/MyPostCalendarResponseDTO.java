package com.kb.youngly.dto.posts;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 로그인 사용자의 인증 캘린더용 게시물 정보입니다.
 * 결과는 postedAt 오름차순으로 반환됩니다.
 */
@Data
public class MyPostCalendarResponseDTO {
    private Long postId;
    private Long roundId;
    private String groupId;
    private String groupName;
    private String photoUrl;
    private String content;
    private String postStatus;

    private Integer likeCount;
    private Integer dislikeCount;

    private LocalDateTime postedAt;
    private LocalDateTime statusChangedAt;
    private String rejectReason;
}
