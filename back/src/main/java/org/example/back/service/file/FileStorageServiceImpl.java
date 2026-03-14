package org.example.back.service.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.config.properties.FileStorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "txt", "doc", "docx"
    );

    private final FileStorageProperties fileStorageProperties;

    @Override
    public String save(MultipartFile file, Long memberId) {
        try {
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                throw new IllegalArgumentException("파일명이 존재하지 않습니다.");
            }

            String extension = extractExtension(originalName);
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("허용되지 않는 파일 확장자입니다: " + extension);
            }
            
            String fileName =
                    UUID.randomUUID() + "_" + StringUtils.cleanPath(originalName);
            Path uploadPath = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath()
                    .normalize();
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            Path targetPath = uploadPath.resolve(fileName);
            file.transferTo(targetPath.toFile());
            
            log.info("[파일 업로드] memberId={}, 파일명={}", memberId, originalName);
            return "/files/" + fileName;
        } catch (IOException e) {
            log.error("파일 저장 중 에러 발생", e);
            throw new RuntimeException("파일 저장 중 에러 발생");
        }
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}
