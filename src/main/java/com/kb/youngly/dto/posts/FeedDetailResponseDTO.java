// FeedDetailResponseDTO.java
// 피드 상세 화면을 위한 세트
// : CommentDTO(댓글 목록), ReactionUserDTO(좋아요 누른 유저 목록), ReactionUserDTO(싫어요 누른 유저 목록)를 각각 리스트(List<>) 형태로 한 번에 묶어서 반환

package com.kb.youngly.dto.posts;

        import lombok.AllArgsConstructor;
        import lombok.Data;
        import lombok.NoArgsConstructor;
        import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedDetailResponseDTO {
    private List<CommentDTO> comments;
    private List<ReactionUserDTO> likers;
    private List<ReactionUserDTO> dislikers;
}
