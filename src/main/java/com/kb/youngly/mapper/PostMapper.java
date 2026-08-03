package com.kb.youngly.mapper;


import com.kb.youngly.vo.post.PostVO;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;

import com.kb.youngly.dto.posts.*;
import org.apache.ibatis.annotations.*;


import java.util.List;

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


    // 1. 특정 라운드(그룹)의 날짜별 피드 리스트 조회 (최신 댓글 1개 포함)
    // : 특정 날짜의 게시글과 작성자 정보를 조인해서 가져오고, 괄호 안의 서브 쿼리를 통해 가장 최근에 달린 댓글 1개를 가져옴
    @Select("""
        SELECT p.post_id, p.photo_url, p.content, p.posted_at, 
               p.like_count, p.dislike_count, p.comment_count, p.post_status,
               u.user_id, u.nickname, u.profile_image_url,
               
               (SELECT content 
                FROM post_comments 
                WHERE post_id = p.post_id 
                ORDER BY created_at DESC 
                LIMIT 1) AS latest_comment_content
                
        FROM posts p
        JOIN users u ON p.user_id = u.user_id
        WHERE p.round_id = #{roundId} 
          AND DATE(p.posted_at) = #{date}
          AND p.post_status != 'NONE'
        ORDER BY p.posted_at DESC
    """)
    List<FeedListResponseDTO> getFeedListByDate(@Param("roundId") Long roundId, @Param("date") String date);

    // 2. 특정 게시글의 전체 댓글 목록 조회 (과거순)
    @Select("""
        SELECT c.post_comment_id, c.content, c.created_at,
               u.user_id, u.nickname, u.profile_image_url
        FROM post_comments c
        JOIN users u ON c.user_id = u.user_id
        WHERE c.post_id = #{postId}
        ORDER BY c.created_at ASC
    """)
    List<CommentDTO> getCommentsByPostId(@Param("postId") Long postId);

    // 3. 특정 게시글에 특정 리액션(LIKE/DISLIKE)을 한 유저 목록 조회
    @Select("""
        SELECT u.user_id, u.nickname, u.profile_image_url
        FROM post_reactions pr
        JOIN users u ON pr.user_id = u.user_id
        WHERE pr.post_id = #{postId} 
          AND pr.reaction_type = #{reactionType}
        ORDER BY pr.created_at DESC
    """)
    List<ReactionUserDTO> getReactionUsersByPostId(@Param("postId") Long postId, @Param("reactionType") String reactionType);

    // 그룹 멤버십 검증 쿼리: 특정 유저가 해당 라운드(round_id)에 참여 이력이 있고, group_users에서 ACTIVE 상태인지 확인
    @Select("""
        SELECT COUNT(*) 
        FROM round_history rh
        JOIN group_users gu ON rh.user_id = gu.user_id
        WHERE rh.round_id = #{roundId} 
          AND rh.user_id = #{userId}
          AND gu.group_user_status = 'ACTIVE'
    """)
    boolean checkGroupMembership(@Param("roundId") Long roundId, @Param("userId") String userId);


    // PostMapper.java 내부에 추가

    // (만약 상세 조회할 때 만들어둔 getPostById가 있다면 안 만들어도 됨!)
    // 4. 특정 게시글의 작성자 등 기본 정보 조회 (본인 검증용)
    @Select("""
        SELECT post_id, user_id, post_status 
        FROM posts 
        WHERE post_id = #{postId}
    """)
    PostDTO getPostById(@Param("postId") Long postId);

    // 5. 중복 평가 여부 확인 (이미 승인/반려를 했는지 카운트)
    @Select("""
        SELECT COUNT(*) 
        FROM post_approvals
        WHERE post_id = #{postId} AND user_id = #{userId}
    """)
    int checkDuplicateApproval(@Param("postId") Long postId, @Param("userId") String userId);

    // 6. 평가 내역 저장 (승인 또는 반려 기록 남기기)
    @Insert("""
        INSERT INTO post_approvals (user_id, post_id, approval_status, reject_reason, created_at, updated_at)
        VALUES (#{dto.userId}, #{postId}, #{dto.approvalStatus}, #{dto.rejectReason}, NOW(), NOW())
    """)
    void insertPostApproval(@Param("postId") Long postId, @Param("dto") PostApprovalRequestDTO dto);

    // 7-1. 승인 시 posts 테이블의 승인 카운트 +1 증가
    @Update("""
        UPDATE posts 
        SET approve_count = approve_count + 1 
        WHERE post_id = #{postId}
    """)
    void incrementApproveCount(@Param("postId") Long postId);

    // 7-2. 반려 시 posts 테이블의 반려 카운트 +1 증가
    @Update("""
        UPDATE posts 
        SET reject_count = reject_count + 1 
        WHERE post_id = #{postId}
    """)
    void incrementRejectCount(@Param("postId") Long postId);
}