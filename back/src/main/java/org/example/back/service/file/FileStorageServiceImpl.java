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
import org.example.back.domain.file.UploadedFile;
import org.example.back.domain.member.Member;
import org.example.back.exception.file.FileErrorCode;
import org.example.back.exception.file.FileException;
import org.example.back.exception.member.MemberErrorCode;
import org.example.back.exception.member.MemberException;
import org.example.back.repository.MemberRepository;
import org.example.back.repository.file.UploadedFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "txt", "doc", "docx"
    );

    private final FileStorageProperties fileStorageProperties;
    private final MemberRepository memberRepository;
    private final UploadedFileRepository uploadedFileRepository;

    @Override
    @Transactional
    public UploadedFile save(MultipartFile file, Long memberId) {
        // 1. 입력 검증
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new FileException(FileErrorCode.FILENAME_MISSING);
        }

        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FileException(FileErrorCode.EXTENSION_NOT_ALLOWED);
        }

        // 2. 업로더 조회 (JWT 통과 직후라 보통 존재. 방어적 검증)
        Member uploader = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.USER_NOT_FOUND));

        // 3. 디스크 저장 (파일명: UUID + 확장자. 원본명은 DB에만 보관)
        String savedPath = UUID.randomUUID() + "." + extension;
        Path uploadPath = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path targetPath = uploadPath.resolve(savedPath);
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            log.error("파일 저장 중 에러 발생 - memberId={}, 파일명={}", memberId, originalName, e);
            throw new FileException(FileErrorCode.FILE_STORAGE_FAILED, e);
        }

        // 4. DB 기록
        UploadedFile saved = uploadedFileRepository.save(
                UploadedFile.of(uploader, savedPath, originalName, extension, file.getSize())
        );

        log.info("[파일 업로드] uploadedFileId={}, uploader={}, 원본명={}, savedPath={}, size={}",
                saved.getId(), memberId, originalName, savedPath, file.getSize());

        return saved;
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}
