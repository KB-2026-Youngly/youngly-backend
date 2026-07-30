package com.kb.youngly.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data // Getter, Setter, toString 등을 알아서 만들어주는 어노테이션
public class PostDTO {
    // 프론트엔드에서 폼 데이터(Form-data)로 넘어올 값들
    private Long roundId;
    private String userId;
    private String content;

    // 진짜 핵심! 클라이언트가 업로드한 이미지 파일을 받아줄 변수
    private MultipartFile imageFile;
}