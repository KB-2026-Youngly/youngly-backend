package com.kb.youngly.dto.posts;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CreatePostRequestDTO {

    // 인증을 등록할 현재 라운드
    private Long roundId;

    // 인증 소감
    private String content;

    // 인증 이미지
    private MultipartFile imageFile;
}