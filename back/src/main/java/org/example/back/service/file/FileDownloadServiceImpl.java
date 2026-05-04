package org.example.back.service.file;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.config.properties.FileStorageProperties;
import org.example.back.domain.file.UploadedFile;
import org.example.back.exception.file.FileErrorCode;
import org.example.back.exception.file.FileException;
import org.example.back.repository.file.UploadedFileRepository;
import org.example.back.repository.message.ChatMessageRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileDownloadServiceImpl implements FileDownloadService {

    // 업로드 시점과 동일한 화이트리스트. 디스크 파일이 어떤 경로로 들어왔든 응답 단계에서 한 번 더 차단.
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "txt", "doc", "docx"
    );

    private final UploadedFileRepository uploadedFileRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final FileStorageProperties fileStorageProperties;

    @Override
    public ResponseEntity<Resource> download(Long fileId, Long memberId) {
        // 1. 파일 메타데이터 조회
        UploadedFile file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.warn("[다운로드] 파일 없음 - fileId={}, memberId={}", fileId, memberId);
                    return new FileException(FileErrorCode.FILE_NOT_FOUND);
                });

        // 2. 권한 검증: 업로더 본인 OR 사용된 채팅방 참여자
        boolean isUploader = file.getUploader().getId().equals(memberId);
        boolean isParticipant = !isUploader
                && chatMessageRepository.existsParticipantAccessByFileIdAndMemberId(fileId, memberId);

        if (!isUploader && !isParticipant) {
            log.warn("[다운로드] 권한 없음 - fileId={}, memberId={}, uploader={}",
                    fileId, memberId, file.getUploader().getId());
            throw new FileException(FileErrorCode.NO_DOWNLOAD_PERMISSION);
        }

        // 3. 확장자 이중 검증 (업로드 시 검증되지만 응답 단계 방어)
        if (!ALLOWED_EXTENSIONS.contains(file.getExtension())) {
            log.error("[다운로드] 화이트리스트 미포함 확장자 - fileId={}, ext={}", fileId, file.getExtension());
            throw new FileException(FileErrorCode.EXTENSION_NOT_ALLOWED);
        }

        // 4. 디스크에서 파일 읽기
        Resource resource = readFromDisk(file);

        // 5. 응답 헤더 구성
        HttpHeaders headers = buildHeaders(file);

        log.info("[다운로드] 성공 - fileId={}, memberId={}, isUploader={}, isParticipant={}",
                fileId, memberId, isUploader, isParticipant);

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    private Resource readFromDisk(UploadedFile file) {
        Path uploadPath = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();
        Path target = uploadPath.resolve(file.getSavedPath()).normalize();

        // 경로 traversal 최후 방어: 정규화 후 부모가 uploadPath 인지 확인
        if (!target.startsWith(uploadPath)) {
            log.error("[다운로드] 경로 이탈 감지 - fileId={}, target={}", file.getId(), target);
            throw new FileException(FileErrorCode.NO_DOWNLOAD_PERMISSION);
        }

        if (!Files.exists(target) || !Files.isReadable(target)) {
            log.error("[다운로드] 디스크 파일 없음/읽기 불가 - fileId={}, path={}", file.getId(), target);
            throw new FileException(FileErrorCode.FILE_NOT_FOUND);
        }

        try {
            return new UrlResource(target.toUri());
        } catch (IOException e) {
            log.error("[다운로드] Resource 생성 실패 - fileId={}", file.getId(), e);
            throw new FileException(FileErrorCode.FILE_READ_FAILED, e);
        }
    }

    private HttpHeaders buildHeaders(UploadedFile file) {
        HttpHeaders headers = new HttpHeaders();

        // Content-Type 추론 (실패 시 octet-stream)
        String contentType = probeContentType(file);
        headers.setContentType(MediaType.parseMediaType(contentType));

        // Content-Disposition: attachment + 한글 파일명 RFC 5987 인코딩
        String encoded = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getOriginalName() + "\"; filename*=UTF-8''" + encoded);

        // MIME sniffing 방어
        headers.add("X-Content-Type-Options", "nosniff");

        headers.setContentLength(file.getSize());

        return headers;
    }

    private String probeContentType(UploadedFile file) {
        try {
            Path target = Paths.get(fileStorageProperties.getUploadDir())
                    .toAbsolutePath().normalize().resolve(file.getSavedPath());
            String detected = Files.probeContentType(target);
            return detected != null ? detected : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}
