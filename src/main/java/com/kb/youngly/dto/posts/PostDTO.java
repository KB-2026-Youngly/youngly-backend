package com.kb.youngly.dto.posts;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data // Getter, Setter, toString 등을 알아서 만들어주는 어노테이션
public class PostDTO {
    // 프론트엔드에서 폼 데이터(Form-data)로 넘어올 값들
    private Long roundId;       // 이 게시글이 속한 라운드(그룹 총인원 계산용)
    private String userId;
    private String content;
    private String postStatus;

    // 진짜 핵심! 클라이언트가 업로드한 이미지 파일을 받아줄 변수
    private MultipartFile imageFile;

    // 과반수 로직을 위한 필드 (Mapper 쿼리와 맵핑됨)
    private int approveCount;   // 현재까지의 승인 수
    private int rejectCount;    // 현재까지의 반려 수

    private int likeCount;
    private int dislikeCount;

    private String photoUrl;
}