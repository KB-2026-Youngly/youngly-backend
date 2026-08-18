package com.kb.youngly.dto.posts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedDetailResponseDTO {

    private String postStatus;
    private Integer approveCount;
    private Integer rejectCount;

    private List<CommentDTO> comments;
    private List<ReactionUserDTO> likers;
    private List<ReactionUserDTO> dislikers;
}