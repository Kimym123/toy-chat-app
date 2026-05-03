package org.example.back.domain.file;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.back.domain.base.BaseTimeEntity;
import org.example.back.domain.member.Member;

/**
 * 업로드된 파일의 메타데이터.
 * 디스크에 저장된 실제 파일은 savedPath 로 추적.
 * ChatMessage 와의 관계는 ChatMessage.file_id (역참조) 로 관리한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "uploaded_file")
public class UploadedFile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 업로더 (다운로드 권한 검증의 기본 주체)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private Member uploader;

    // 디스크에 저장된 파일명 (UUID 기반, 외부 노출용 식별자가 아님)
    @Column(name = "saved_path", nullable = false, length = 255)
    private String savedPath;

    // 클라이언트가 올린 원본 파일명 (다운로드 시 Content-Disposition 에 사용)
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    // 확장자 (소문자, 점 없이 저장. 예: jpg, png, pdf)
    @Column(nullable = false, length = 16)
    private String extension;

    // 파일 크기 (바이트)
    @Column(nullable = false)
    private Long size;

    public static UploadedFile of(Member uploader, String savedPath, String originalName,
                                  String extension, Long size) {
        return UploadedFile.builder()
                .uploader(uploader)
                .savedPath(savedPath)
                .originalName(originalName)
                .extension(extension)
                .size(size)
                .build();
    }
}
