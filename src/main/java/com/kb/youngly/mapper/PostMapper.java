package com.kb.youngly.mapper;

import com.kb.youngly.vo.PostVO;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper {
    // DB의 빈 인증 게시글(NONE) 상태를 PENDING으로 수정(UPDATE)하는 메서드

    @Update("""
    UPDATE posts 
    SET photo_url = #{photoUrl},
        content = #{content},
        post_status = 'PENDING',
        posted_at = NOW()
    WHERE round_id = #{roundId} 
      AND user_id = #{userId} 
      AND post_status = 'NONE'
""")
    int updatePost(PostVO postVO);
}