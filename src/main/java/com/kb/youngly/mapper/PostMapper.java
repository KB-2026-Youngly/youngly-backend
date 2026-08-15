package com.kb.youngly.mapper;

import com.kb.youngly.vo.post.PostVO;
import com.kb.youngly.dto.posts.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper {

    // DB의 빈 인증 게시글(NONE) 상태를 PENDING으로 수정(UPDATE)하는 메서드
    int updatePost(PostVO postVO);

    // 오늘 미인증 상태로 만들어진 빈 게시글 ID 조회
    Long findTodayEmptyPostId(
            @Param("roundId") Long roundId,
            @Param("userId") String userId
    );

    // 오늘 이미 업로드한 인증 게시글 개수
    int countTodayUploadedPost(
            @Param("roundId") Long roundId,
            @Param("userId") String userId
    );

    // 미리 만들어진 NONE 게시물이 없을 때 새 게시글 생성
    int insertPost(PostVO postVO);

    // 1. 특정 라운드(그룹)의 날짜별 피드 리스트 조회 (최신 댓글 1개 포함)
    List<FeedListResponseDTO> getFeedListByDate(@Param("roundId") Long roundId, @Param("date") String date);

    // 2. 특정 게시글의 전체 댓글 목록 조회 (과거순)
    List<CommentDTO> getCommentsByPostId(@Param("postId") Long postId);

    // 3. 특정 게시글에 특정 리액션(LIKE/DISLIKE)을 한 유저 목록 조회
    List<ReactionUserDTO> getReactionUsersByPostId(@Param("postId") Long postId, @Param("reactionType") String reactionType);

    // 그룹 멤버십 검증 쿼리: 특정 유저가 해당 라운드(round_id)에 참여 이력이 있고, group_users에서 ACTIVE 상태인지 확인
    boolean checkGroupMembership(@Param("roundId") Long roundId, @Param("userId") String userId);

    // 게시물 승인 및 반려 API - 특정 게시글의 작성자 등 기본 정보 조회 (본인 검증용)
    PostDTO getPostById(@Param("postId") Long postId);

    // 5. 중복 평가 여부 확인 (이미 승인/반려를 했는지 카운트)
    int checkDuplicateApproval(@Param("postId") Long postId, @Param("userId") String userId);

    // 6. 평가 내역 저장 (승인 또는 반려 기록 남기기)
    void insertPostApproval(@Param("postId") Long postId, @Param("dto") PostApprovalRequestDTO dto);

    // 7-1. 승인 시 posts 테이블의 승인 카운트 +1 증가
    void incrementApproveCount(@Param("postId") Long postId);

    // 7-2. 반려 시 posts 테이블의 반려 카운트 +1 증가
    void incrementRejectCount(@Param("postId") Long postId);

    // 8. 특정 라운드에 속한 그룹의 전체 활성 멤버 수 조회 (과반수 계산용)
    int getTotalGroupMembersByRoundId(@Param("roundId") Long roundId);

    // 9. 과반수 투표 달성 시 게시글의 최종 상태 및 상태변경시간 업데이트
    void updatePostStatus(@Param("postId") Long postId, @Param("status") String status);

    // 10. 시간 초과된 PENDING 게시글 일괄 자동 승인 처리 (업데이트된 행의 개수 반환)
    int updatePostsToAutoApproved();
}