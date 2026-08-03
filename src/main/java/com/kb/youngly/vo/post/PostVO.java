package com.kb.youngly.vo;

import com.kb.youngly.enums.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostVO {
    private Long postId;
    private Long roundId;
    private String userId;
    private String photoUrl;
    private String content;
    private PostStatus postStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime postedAt;
    private LocalDateTime statusChangedAt;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;
    private Integer rejectCount;
    private Integer approveCount;
}
