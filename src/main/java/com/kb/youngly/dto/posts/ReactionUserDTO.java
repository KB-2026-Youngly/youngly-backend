// ReactionUserDTO.java (좋아요/싫어요 누른 유저 정보)
package com.kb.youngly.dto.posts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionUserDTO {
    private String userId;
    private String nickname;
    private String profileImageUrl;
}