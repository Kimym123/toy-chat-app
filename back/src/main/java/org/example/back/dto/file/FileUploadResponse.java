package org.example.back.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.back.domain.file.UploadedFile;

@Schema(description = "파일 업로드 응답 DTO")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileUploadResponse {

    @Schema(description = "업로드된 파일의 ID (메시지 전송 시 fileId 로 사용)", example = "42")
    private Long fileId;

    @Schema(description = "다운로드 URL (인증 필요)", example = "/api/files/42")
    private String downloadUrl;

    @Schema(description = "원본 파일명", example = "내사진.jpg")
    private String originalName;

    @Schema(description = "파일 크기 (바이트)", example = "204800")
    private Long size;

    public static FileUploadResponse from(UploadedFile file) {
        return FileUploadResponse.builder()
                .fileId(file.getId())
                .downloadUrl("/api/files/" + file.getId())
                .originalName(file.getOriginalName())
                .size(file.getSize())
                .build();
    }
}
