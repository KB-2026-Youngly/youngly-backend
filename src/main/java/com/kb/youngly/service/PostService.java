package com.kb.youngly.service;

import com.kb.youngly.dto.posts.*;
import com.kb.youngly.mapper.PostMapper;
import com.kb.youngly.mapper.RoundMapper;
import com.kb.youngly.util.FileUploadUtil;
import com.kb.youngly.vo.post.PostVO;
import com.kb.youngly.vo.round.RoundVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.kb.youngly.dto.posts.CreatePostRequestDTO;
import com.kb.youngly.dto.posts.CreatePostResponseDTO;
import com.kb.youngly.enums.PostStatus;
import com.kb.youngly.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;

import com.kb.youngly.enums.ReactionType;
import com.kb.youngly.vo.post.PostCommentVO;
import com.kb.youngly.vo.post.PostReactionVO;

import java.time.LocalDateTime;
import java.util.List;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor // final이 붙은 변수들을 알아서 조립해 주는 어노테이션
public class PostService {

    private final PostMapper postMapper;
    private final FileUploadUtil fileUploadUtil;
    private final NotificationService notificationService;
    private final RoundMapper roundMapper;


    @Transactional
    public CreatePostResponseDTO createPost(
            String userId,
            CreatePostRequestDTO request
    ) {
        validateCreatePostRequest(request);

        Long roundId = request.getRoundId();

        // 라운드 존재 여부 및 인증 가능 기간 확인
        RoundVO round = roundMapper.findRoundById(roundId);

        if (round == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 라운드입니다."
            );
        }

        LocalDate today = LocalDate.now();

        if (today.isBefore(round.getStartDate())
                || today.isAfter(round.getEndDate())) {
            throw new IllegalArgumentException(
                    "현재 인증 가능한 기간이 아닙니다."
            );
        }

        // 해당 라운드의 활성 참여자인지 확인
        boolean isMember =
                postMapper.checkGroupMembership(roundId, userId);

        if (!isMember) {
            throw new IllegalArgumentException(
                    "해당 챌린지에 인증을 등록할 권한이 없습니다."
            );
        }

        // 하루에 한 번만 인증 가능
        int uploadedCount =
                postMapper.countTodayUploadedPost(roundId, userId);

        if (uploadedCount > 0) {
            throw new IllegalArgumentException(
                    "오늘 인증 게시글을 이미 등록했습니다."
            );
        }

        String content = request.getContent() == null
                ? ""
                : request.getContent().trim();

        String savedFileName =
                fileUploadUtil.saveFile(request.getImageFile());

        PostVO post = PostVO.builder()
                .roundId(roundId)
                .userId(userId)
                .content(content)
                .photoUrl(savedFileName)
                .postStatus(PostStatus.PENDING)
                .build();

        try {
            // 매일 미리 만들어진 NONE 게시물이 있는지 확인
            Long emptyPostId =
                    postMapper.findTodayEmptyPostId(roundId, userId);

            if (emptyPostId != null) {
                post.setPostId(emptyPostId);

                int updatedCount = postMapper.updatePost(post);

                if (updatedCount != 1) {
                    throw new IllegalStateException(
                            "인증 게시글 상태 변경에 실패했습니다."
                    );
                }
            } else {
                // NONE 게시물을 생성하는 스케줄러가 아직 없는 경우
                // 새 게시글을 바로 생성
                int insertedCount = postMapper.insertPost(post);

                if (insertedCount != 1 || post.getPostId() == null) {
                    throw new IllegalStateException(
                            "인증 게시글 등록에 실패했습니다."
                    );
                }
            }
        } catch (RuntimeException exception) {
            // DB 저장 실패 시 먼저 저장한 파일 제거
            fileUploadUtil.deleteFile(savedFileName);
            throw exception;
        }

        try {
            String nickname = postMapper.findUserNickname(userId);

            List<String> notificationUserIds =
                    postMapper.findNotificationUserIds(roundId, userId);

            String notificationContent =
                    nickname + "님이 인증 게시물을 업로드했습니다.";

            for (String notificationUserId : notificationUserIds) {
                try {
                    notificationService.createNotification(
                            notificationUserId,
                            NotificationType.POST_UPLOAD,
                            notificationContent
                    );
                } catch (Exception exception) {
                    log.warn(
                            "인증 게시물 업로드 알림 생성 실패. " +
                                    "postId={}, roundId={}, targetUserId={}",
                            post.getPostId(),
                            roundId,
                            notificationUserId,
                            exception
                    );
                }
            }
        } catch (Exception exception) {
            log.warn(
                    "인증 게시물 업로드 알림 대상 조회 실패. " +
                            "postId={}, roundId={}, userId={}",
                    post.getPostId(),
                    roundId,
                    userId,
                    exception
            );
        }

        return CreatePostResponseDTO.builder()
                .postId(post.getPostId())
                .roundId(post.getRoundId())
                .userId(post.getUserId())
                .content(post.getContent())
                .photoUrl("/uploads/" + savedFileName)
                .postStatus(PostStatus.PENDING)
                .build();
    }

    // 피드 목록 조회 로직
    // : DB에서 데이터를 가져오기 직전에 권한 검증 로직을 실행해. 멤버가 아니라면 에러.
    public List<FeedListResponseDTO> getFeedList(Long roundId, String date, String currentUserId) {
        boolean isMember = postMapper.checkGroupMembership(roundId, currentUserId);
        System.out.println("[DEBUG] 멤버십 검증 결과 (isMember) : " + isMember + " / userId : " + currentUserId); // 👈 요거 찍어보기

        if (!isMember) {
            throw new AccessDeniedException(
                    "해당 그룹에 접근 권한이 없거나 활성 상태가 아닙니다."
            );
        }

        return postMapper.getFeedListByDate(
                roundId,
                date,
                currentUserId
        );
    }

    /**
     * 로그인 사용자의 인증 캘린더를 위한 기간별 게시물 조회입니다.
     * posted_at 기준으로 조회하며, 현재 라운드 여부와 무관하게 과거 기록도 포함합니다.
     */
    @Transactional(readOnly = true)
    public List<MyPostCalendarResponseDTO> getMyPostsByPeriod(
            String userId,
            LocalDate from,
            LocalDate to,
            String groupId
    ) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("조회 시작일과 종료일은 필수입니다.");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("조회 시작일은 종료일보다 늦을 수 없습니다.");
        }

        String normalizedGroupId = groupId == null ? null : groupId.trim();
        if (normalizedGroupId != null && normalizedGroupId.isEmpty()) {
            normalizedGroupId = null;
        }

        if (normalizedGroupId != null
                && !postMapper.checkGroupParticipationHistory(normalizedGroupId, userId)) {
            throw new AccessDeniedException("해당 그룹의 참여 이력이 없습니다.");
        }

        return postMapper.getMyPostsByPeriod(userId, from, to, normalizedGroupId);
    }

    // 피드 상세 조회 서비스
    // : 게시글 상세 정보(댓글 + 공감 내역)를 한 번에 조립해서 반환
    @Transactional(readOnly = true)
    public FeedDetailResponseDTO getFeedDetails(
            Long postId,
            String userId
    ) {
        PostDTO post =
                getInteractivePost(postId, userId);

        List<CommentDTO> comments =
                postMapper.getCommentsByPostId(postId);

        List<ReactionUserDTO> likers =
                postMapper.getReactionUsersByPostId(postId, "LIKE");

        List<ReactionUserDTO> dislikers =
                postMapper.getReactionUsersByPostId(postId, "DISLIKE");

        return FeedDetailResponseDTO.builder()
                .postStatus(post.getPostStatus())
                .approveCount(post.getApproveCount())
                .rejectCount(post.getRejectCount())
                .comments(comments)
                .likers(likers)
                .dislikers(dislikers)
                .build();
    }

    // 게시물 승인 or 반려 프로세스
    // : 방어 로직 1 - 본인 평가 방지
    // : 방어 로직 2 - 중복 평가 방지
    @Transactional
        public void processPostApproval(
                Long postId,
                String userId,
                PostApprovalRequestDTO requestDTO
        ) {

        PostDTO post = getAccessiblePost(postId, userId);

        if (!"PENDING".equals(post.getPostStatus())) {
            throw new IllegalArgumentException(
                    "이미 승인 또는 반려가 완료된 게시물입니다."
            );
        }

        LocalDateTime reviewDeadline =
                post.getPostedAt()
                        .toLocalDate()
                        .plusDays(2)
                        .atStartOfDay();

        if (!LocalDateTime.now().isBefore(reviewDeadline)) {
            throw new IllegalArgumentException(
                    "인증 게시물의 승인/반려 가능 시간이 종료되었습니다."
            );
        }

        if (post.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    "본인의 인증 게시물은 스스로 평가할 수 없습니다."
            );
        }

        if (requestDTO == null
                || requestDTO.getApprovalStatus() == null) {
            throw new IllegalArgumentException(
                    "승인 또는 반려를 선택해 주세요."
            );
        }

        String approvalStatus =
                requestDTO.getApprovalStatus().trim().toUpperCase();

        if (!"APPROVE".equals(approvalStatus)
                && !"REJECT".equals(approvalStatus)) {
            throw new IllegalArgumentException(
                    "승인 상태는 APPROVE 또는 REJECT만 가능합니다."
            );
        }

        requestDTO.setApprovalStatus(approvalStatus);

        if ("REJECT".equals(approvalStatus)
                && (requestDTO.getRejectReason() == null
                || requestDTO.getRejectReason().trim().isEmpty())) {
            throw new IllegalArgumentException(
                    "반려 시 사유를 반드시 입력해야 합니다."
            );
        }

        int duplicateCheck =
                postMapper.checkDuplicateApproval(postId, userId);

        if (duplicateCheck > 0) {
            throw new IllegalArgumentException(
                    "이미 해당 게시물에 대한 평가를 완료했습니다."
            );
        }

        postMapper.insertPostApproval(
                postId,
                userId,
                requestDTO
        );

        if ("APPROVE".equals(approvalStatus)) {
            postMapper.incrementApproveCount(postId);
        } else {
            postMapper.incrementRejectCount(postId);
        }

        // ... 기존 로직 (중복 투표 검사, post_approvals 테이블 인서트, 카운트 +1 증가 등) ...

        // ==========================================
        // 💡 [추가할 로직] 과반수 투표 판정 및 상태 업데이트
        // ==========================================

        // 1. 방금 투표(카운트 +1)가 반영된 최신 게시글 데이터를 다시 조회해 와!
        PostDTO updatedPost = postMapper.getPostById(postId);

        // 2. 이 게시글이 속한 라운드의 전체 그룹 멤버(ACTIVE) 수 조회
        int totalMembers = postMapper.getTotalGroupMembersByRoundId(
                updatedPost.getRoundId()
        );

// 작성자 본인은 투표할 수 없으므로 투표 가능 인원은 총 인원 - 1
        int eligibleVoters = Math.max(totalMembers - 1, 0);

// 투표 가능 인원의 절반을 초과해야 과반수
        int majorityThreshold = eligibleVoters / 2;

        if (updatedPost.getApproveCount() > majorityThreshold) {
            postMapper.updatePostStatus(postId, "APPROVED");

        } else if (updatedPost.getRejectCount() > majorityThreshold) {
            postMapper.updatePostStatus(postId, "REJECTED");
        }
        // 둘 다 과반수를 못 넘었으면? 아직 투표가 진행 중인 거니까 그냥 종료(PASS)!
    }

    private void validateCreatePostRequest(
            CreatePostRequestDTO request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "인증 게시글 정보가 없습니다."
            );
        }

        if (request.getRoundId() == null) {
            throw new IllegalArgumentException(
                    "인증할 그룹을 선택해 주세요."
            );
        }

        MultipartFile imageFile = request.getImageFile();

        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException(
                    "인증 사진을 등록해 주세요."
            );
        }

        String content = request.getContent();

        if (content != null && content.trim().length() > 20) {
            throw new IllegalArgumentException(
                    "인증 내용은 20자 이하로 입력해 주세요."
            );
        }
    }

    @Transactional
    public CommentDTO createComment(
            Long postId,
            String userId,
            CreateCommentRequestDTO request
    ) {
        PostDTO post =
                getInteractivePost(postId, userId);

        String content = request == null || request.getContent() == null
                ? ""
                : request.getContent().trim();

        if (content.isEmpty()) {
            throw new IllegalArgumentException(
                    "댓글 내용을 입력해 주세요."
            );
        }

        if (content.length() > 100) {
            throw new IllegalArgumentException(
                    "댓글은 100자 이하로 입력해 주세요."
            );
        }

        PostCommentVO comment = PostCommentVO.builder()
                .postId(postId)
                .userId(userId)
                .content(content)
                .build();

        int insertedCount =
                postMapper.insertPostComment(comment);

        if (insertedCount != 1
                || comment.getPostCommentId() == null) {
            throw new IllegalStateException(
                    "댓글 등록에 실패했습니다."
            );
        }

        postMapper.incrementCommentCount(postId);

        return postMapper.getCommentById(
                comment.getPostCommentId()
        );
    }

    @Transactional
    public PostReactionResponseDTO setReaction(
            Long postId,
            String userId,
            PostReactionRequestDTO request
    ) {
        getInteractivePost(postId, userId);

        if (request == null
                || request.getReactionType() == null) {
            throw new IllegalArgumentException(
                    "반응 종류를 선택해 주세요."
            );
        }

        ReactionType reactionType;

        try {
            reactionType = ReactionType.valueOf(
                    request.getReactionType()
                            .trim()
                            .toUpperCase()
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "반응은 LIKE 또는 DISLIKE만 가능합니다."
            );
        }

        if (reactionType == ReactionType.PENDING) {
            throw new IllegalArgumentException(
                    "반응은 LIKE 또는 DISLIKE만 가능합니다."
            );
        }

        PostReactionVO existingReaction =
                postMapper.getPostReaction(postId, userId);

        if (existingReaction == null) {
            PostReactionVO reaction =
                    PostReactionVO.builder()
                            .postId(postId)
                            .userId(userId)
                            .reactionType(reactionType)
                            .build();

            postMapper.insertPostReaction(reaction);
        } else if (existingReaction.getReactionType()
                != reactionType) {
            postMapper.updatePostReaction(
                    postId,
                    userId,
                    reactionType.name()
            );
        }

        postMapper.syncPostReactionCounts(postId);

        return buildReactionResponse(
                postId,
                reactionType.name()
        );
    }

    @Transactional
    public PostReactionResponseDTO deleteReaction(
            Long postId,
            String userId
    ) {
        getInteractivePost(postId, userId);

        postMapper.deletePostReaction(postId, userId);
        postMapper.syncPostReactionCounts(postId);

        return buildReactionResponse(postId, null);
    }

    private PostDTO getAccessiblePost(
            Long postId,
            String userId
    ) {
        PostDTO post = postMapper.getPostById(postId);

        if (post == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 인증 게시글입니다."
            );
        }

        if ("NONE".equals(post.getPostStatus())) {
            throw new IllegalStateException(
                    "아직 인증되지 않은 게시글입니다."
            );
        }

        boolean isMember =
                postMapper.checkGroupMembership(
                        post.getRoundId(),
                        userId
                );

        if (!isMember) {
            throw new AccessDeniedException(
                    "해당 게시글에 접근할 권한이 없습니다."
            );
        }

        return post;
    }

    private PostDTO getInteractivePost(
            Long postId,
            String userId
    ) {
        PostDTO post =
                getAccessiblePost(postId, userId);

        boolean isAuthor =
                userId.equals(post.getUserId());

        boolean hasVoted =
                postMapper.checkDuplicateApproval(
                        postId,
                        userId
                ) > 0;

        if (!isAuthor && !hasVoted) {
            throw new AccessDeniedException(
                    "승인 또는 반려 투표 후 게시글에 반응할 수 있습니다."
            );
        }

        return post;
    }

    private PostReactionResponseDTO buildReactionResponse(
            Long postId,
            String myReaction
    ) {
        PostDTO updatedPost =
                postMapper.getPostById(postId);

        return PostReactionResponseDTO.builder()
                .myReaction(myReaction)
                .likeCount(updatedPost.getLikeCount())
                .dislikeCount(updatedPost.getDislikeCount())
                .build();
    }

    @Transactional
    public void deletePost(Long postId, String userId) {
        PostDTO post = postMapper.getPostById(postId);

        if (post == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 인증 게시글입니다."
            );
        }

        if (!userId.equals(post.getUserId())) {
            throw new AccessDeniedException(
                    "본인이 작성한 인증 게시글만 삭제할 수 있습니다."
            );
        }

        if ("NONE".equals(post.getPostStatus())) {
            throw new IllegalStateException(
                    "이미 삭제된 인증 게시글입니다."
            );
        }

        // 외래키 오류를 막기 위해 자식 데이터부터 제거
        postMapper.deletePostComments(postId);
        postMapper.deletePostReactions(postId);
        postMapper.deletePostApprovals(postId);

        int updatedCount =
                postMapper.resetPostForReupload(postId, userId);

        if (updatedCount != 1) {
            throw new IllegalStateException(
                    "인증 게시글 삭제에 실패했습니다."
            );
        }

        // 실제 업로드 사진도 제거
        fileUploadUtil.deleteFile(post.getPhotoUrl());
    }

}
