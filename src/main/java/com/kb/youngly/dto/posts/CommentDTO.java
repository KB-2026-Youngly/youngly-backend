// CommentDTO.java (댓글 DTO)

package com.kb.youngly.dto.posts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long postCommentId;
    private String content;
    private LocalDateTime createdAt;
    private String userId;
    private String nickname;
    private String profileImageUrl;
}