package com.kb.youngly.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Component
public class FileUploadUtil {

    private static final long MAX_IMAGE_SIZE =
            10L * 1024L * 1024L;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".webp");

    @Value("${upload.location}")
    private String uploadLocation;

    public String saveFile(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String savedFilename =
                UUID.randomUUID() + extension;

        File directory = new File(uploadLocation);

        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException(
                    "업로드 폴더를 생성할 수 없습니다."
            );
        }

        File savedFile =
                new File(directory, savedFilename);

        try {
            file.transferTo(savedFile);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "이미지 저장 중 오류가 발생했습니다.",
                    exception
            );
        }

        return savedFilename;
    }

    public void deleteFile(String savedFilename) {
        if (savedFilename == null || savedFilename.isBlank()) {
            return;
        }

        // 디렉터리 이동 문자열 방지
        String safeFilename =
                new File(savedFilename).getName();

        File file =
                new File(uploadLocation, safeFilename);

        if (file.exists() && !file.delete()) {
            System.err.println(
                    "[WARN] 업로드 파일 삭제 실패: "
                            + file.getAbsolutePath()
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "업로드된 이미지가 없습니다."
            );
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                    "이미지는 최대 10MB까지 업로드할 수 있습니다."
            );
        }

        String extension =
                extractExtension(file.getOriginalFilename());

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "jpg, jpeg, png, webp 이미지만 업로드할 수 있습니다."
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException(
                    "이미지 파일만 업로드할 수 있습니다."
            );
        }
    }

    private String extractExtension(String filename) {
        if (filename == null ||
                !filename.contains(".")) {
            throw new IllegalArgumentException(
                    "파일 확장자를 확인할 수 없습니다."
            );
        }

        return filename
                .substring(filename.lastIndexOf("."))
                .toLowerCase();
    }
}