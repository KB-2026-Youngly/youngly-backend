package com.kb.youngly.dto.posts;

import com.kb.youngly.enums.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostResponseDTO {

    private Long postId;
    private Long roundId;
    private String userId;
    private String content;
    private String photoUrl;
    private PostStatus postStatus;
}