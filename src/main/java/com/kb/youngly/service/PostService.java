package com.kb.youngly.service;

import com.kb.youngly.dto.posts.*;
import com.kb.youngly.mapper.PostMapper;
import com.kb.youngly.util.FileUploadUtil;
import com.kb.youngly.vo.post.PostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.kb.youngly.dto.posts.CreatePostRequestDTO;
import com.kb.youngly.dto.posts.CreatePostResponseDTO;
import com.kb.youngly.enums.PostStatus;
import com.kb.youngly.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor // final이 붙은 변수들을 알아서 조립해 주는 어노테이션
public class PostService {

    private final PostMapper postMapper;
    private final FileUploadUtil fileUploadUtil;
    private final NotificationService notificationService;

    @Transactional
    public CreatePostResponseDTO createPost(
            String userId,
            CreatePostRequestDTO request
    ) {
        validateCreatePostRequest(request);

        Long roundId = request.getRoundId();

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
            throw new RuntimeException("해당 그룹에 접근 권한이 없거나 활성 상태가 아닙니다.");
        }

        return postMapper.getFeedListByDate(roundId, date);
    }

    // 피드 상세 조회 서비스
    // : 게시글 상세 정보(댓글 + 공감 내역)를 한 번에 조립해서 반환
    public FeedDetailResponseDTO getFeedDetails(Long postId) {
        // 1. 댓글 목록 긁어오기
        List<CommentDTO> comments = postMapper.getCommentsByPostId(postId);

        // 2. 좋아요 누른 유저 목록 긁어오기 (Enum 타입에 맞춰서 'LIKE' 파라미터 전달)
        List<ReactionUserDTO> likers = postMapper.getReactionUsersByPostId(postId, "LIKE");

        // 3. 싫어요 누른 유저 목록 긁어오기
        List<ReactionUserDTO> dislikers = postMapper.getReactionUsersByPostId(postId, "DISLIKE");

        // 4. 하나의 종합 DTO로 포장해서 리턴
        return new FeedDetailResponseDTO(comments, likers, dislikers);
    }

    // 게시물 승인 or 반려 프로세스
    // : 방어 로직 1 - 본인 평가 방지
    // : 방어 로직 2 - 중복 평가 방지
    @Transactional // 둘 중 하나라도 쿼리 실패 시 롤백시키기 위한 애노테이션
    public void processPostApproval(Long postId, PostApprovalRequestDTO requestDTO) {

        // 1. 게시글 존재 여부 및 작성자 정보 가져오기
        PostDTO post = postMapper.getPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("존재하지 않는 인증 게시물입니다.");
        }

        // 2. 방어 로직: 본인 게시물 스스로 평가 불가
        if (post.getUserId().equals(requestDTO.getUserId())) {
            throw new IllegalArgumentException("본인의 인증 게시물은 스스로 평가할 수 없습니다.");
        }

        // 2-2. 방어 로직: 아직 인증샷이 안올라온 게시글 (NONE) 차단
        if ("NONE".equals(post.getPostStatus())) {
            throw new IllegalStateException("아직 인증이 올라오지 않은 빈 게시글은 평가할 수 없습니다.");
        }

        // 3. 방어 로직: 반려(REJECT) 시 사유 필수
        if ("REJECT".equals(requestDTO.getApprovalStatus())) {
            if (requestDTO.getRejectReason() == null || requestDTO.getRejectReason().trim().isEmpty()) {
                throw new IllegalArgumentException("반려 시 사유를 반드시 입력해야 합니다.");
            }
        }

        // 4. 방어 로직: 이미 평가한 내역이 있는지 중복 검증 (UNIQUE 제약조건 위배 방지)
        int duplicateCheck = postMapper.checkDuplicateApproval(postId, requestDTO.getUserId());
        if (duplicateCheck > 0) {
            throw new IllegalArgumentException("이미 해당 게시물에 대한 평가를 완료했습니다.");
        }

        // 5. 평가 내역 저장 (post_approvals 테이블 INSERT)
        postMapper.insertPostApproval(postId, requestDTO);

        // 6. 게시글 카운트 업데이트 (posts 테이블 UPDATE)
        if ("APPROVE".equals(requestDTO.getApprovalStatus())) {
            postMapper.incrementApproveCount(postId);
        } else if ("REJECT".equals(requestDTO.getApprovalStatus())) {
            postMapper.incrementRejectCount(postId);
        }

        // ... 기존 로직 (중복 투표 검사, post_approvals 테이블 인서트, 카운트 +1 증가 등) ...

        // ==========================================
        // 💡 [추가할 로직] 과반수 투표 판정 및 상태 업데이트
        // ==========================================

        // 1. 방금 투표(카운트 +1)가 반영된 최신 게시글 데이터를 다시 조회해 와!
        PostDTO updatedPost = postMapper.getPostById(postId);

        // 2. 이 게시글이 속한 라운드의 전체 그룹 멤버(ACTIVE) 수 조회
        int totalMembers = postMapper.getTotalGroupMembersByRoundId(updatedPost.getRoundId());

        // 3. 과반수 기준치 계산 (정수 나눗셈)
        // 5명이면 5/2 = 2 (3명부터 과반수)
        // 4명이면 4/2 = 2 (3명부터 과반수)
        int majorityThreshold = totalMembers / 2;

        // 4. 과반수 달성 여부 체크 후 게시물 최종 상태 업데이트
        if (updatedPost.getApproveCount() > majorityThreshold) {
            // 승인 카운트가 과반수를 넘으면? APPROVED(승인) 확정!
            postMapper.updatePostStatus(postId, "APPROVED");

        } else if (updatedPost.getRejectCount() > majorityThreshold) {
            // 반려 카운트가 과반수를 넘으면? REJECTED(반려) 확정!
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

        if (content != null && content.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "인증 소감은 500자 이하로 입력해 주세요."
            );
        }
    }

}