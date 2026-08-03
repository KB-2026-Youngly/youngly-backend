package com.kb.youngly.service;

import com.kb.youngly.dto.posts.*;
import com.kb.youngly.mapper.PostMapper;
import com.kb.youngly.util.FileUploadUtil;
import com.kb.youngly.vo.post.PostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor // final이 붙은 변수들을 알아서 조립해 주는 마법의 어노테이션!
public class PostService {

    private final PostMapper postMapper;
    private final FileUploadUtil fileUploadUtil;

    @Transactional // 파일 저장은 됐는데 DB 저장이 실패하면 둘 다 원상복구 시켜주는 안전장치
    public void createPost(PostDTO postDTO) {
        // 1. DTO에서 클라이언트가 보낸 사진 파일만 쏙 빼오기
        MultipartFile imageFile = postDTO.getImageFile();
        String savedFileName = null;

        // 2. 파일이 있으면 유틸리티를 통해 로컬에 저장하고 파일명(랜덤이름.jpg) 받아오기
        if (imageFile != null && !imageFile.isEmpty()) {
            savedFileName = fileUploadUtil.saveFile(imageFile);
        } else {
            // 인증 게시물인데 사진이 없으면 얄짤없이 에러 뱉기!
            throw new RuntimeException("인증용 이미지가 반드시 필요합니다.");
        }

        // 3. DB에 저장할 VO 객체 그릇에 차곡차곡 담기
        PostVO postVO = new PostVO();
        postVO.setRoundId(postDTO.getRoundId());
        postVO.setUserId(postDTO.getUserId());
        postVO.setContent(postDTO.getContent());
        postVO.setPhotoUrl(savedFileName); // 아까 유틸에서 반환받은 파일명 셋팅

        // 4. 꽉 찬 VO 그릇을 Mapper에게 넘겨서 DB에 최종 UPDATE!
        postMapper.updatePost(postVO);
    }

    // 피드 목록 조회 로직
    // : DB에서 데이터를 가져오기 직전에 권한 검증 로직을 실행해. 멤버가 아니라면 에러를 던져서 철벽을 쳐버려.
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

        // 4. 하나의 종합 DTO로 포장해서 리턴!
        return new FeedDetailResponseDTO(comments, likers, dislikers);
    }

    // PostService.java 내부에 추가
    @Transactional // 둘 중 하나라도 쿼리 실패 시 롤백시키기 위한 마법의 애노테이션!
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
    }

}