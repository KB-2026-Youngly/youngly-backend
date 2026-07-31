package com.kb.youngly.vo.post;

import com.kb.youngly.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReactionVO {
    private Long reactionId;
    private Long postId;
    private String userId;
    private ReactionType reactionType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
