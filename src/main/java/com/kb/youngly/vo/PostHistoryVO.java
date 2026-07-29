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
public class PostHistoryVO {
    private Long postHistoryId;
    private Long postId;
    private String photoUrl;
    private String content;
    private PostStatus postStatus;
    private LocalDateTime createdAt;
}
