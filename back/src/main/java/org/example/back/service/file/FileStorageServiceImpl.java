package org.example.back.service.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    
    private final FileStorageProperties fileStorageProperties;
    
    @Override
    public String save(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                throw new IllegalArgumentException("파일명이 존재하지 않습니다.");
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
            
            return "/files/" + fileName;
        } catch (IOException e) {
            log.error("파일 저장 중 에러 발생", e);
            throw new RuntimeException("파일 저장 중 에러 발생");
        }
    }
}
