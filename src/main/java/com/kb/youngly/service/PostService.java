package com.kb.youngly.service;

import com.kb.youngly.dto.PostDTO;
import com.kb.youngly.mapper.PostMapper;
import com.kb.youngly.util.FileUploadUtil;
import com.kb.youngly.vo.PostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
}