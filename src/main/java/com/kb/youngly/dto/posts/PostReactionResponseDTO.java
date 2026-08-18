package com.kb.youngly.dto.posts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReactionResponseDTO {
    private String myReaction;
    private int likeCount;
    private int dislikeCount;
}