package com.kb.youngly.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Component
public class FileUploadUtil {

    // application.properties에 적어둔 저장 경로
    @Value("${upload.location}")
    private String uploadLocation;

    public String saveFile(MultipartFile file) {
        // 1. 파일이 비어있는지 확인
        if (file.isEmpty()) {
            throw new RuntimeException("업로드된 파일이 없습니다.");
        }

        String originalFilename = file.getOriginalFilename();

        // 2. 파일 확장자 추출 및 소문자 변환
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        // 3. 확장자 검사 (jpg, jpeg, png만 허용)
        if (!extension.equals(".jpg") && !extension.equals(".jpeg") && !extension.equals(".png")) {
            throw new RuntimeException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png만 가능)");
        }

        // 4. 고유한 파일명 생성 (UUID 활용)
        // 사용자들이 올리는 파일 이름이 우연히 똑같은 경우를 막기 위함
        String savedFilename = UUID.randomUUID().toString() + extension;

        // 5. 폴더가 없으면 새로 생성
        File directory = new File(uploadLocation);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 6. 실제 저장할 파일 객체 생성
        File savedFile = new File(uploadLocation, savedFilename);

        // 7. 지정한 경로로 파일 물리적 저장
        try {
            file.transferTo(savedFile);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 서버에 오류가 발생했습니다.");
        }

        // 8. 🚨 수정된 부분: 맥북 전체 경로 대신 '파일명'만 반환!
        // 이렇게 해야 DB photo_url 컬럼에 '랜덤이름.jpg'로 예쁘게 들어가고 나중에 클라우드 연동도 쉬움
        return savedFilename;
    }
}