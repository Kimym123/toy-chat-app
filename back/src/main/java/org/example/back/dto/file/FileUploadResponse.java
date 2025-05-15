package org.example.back.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "파일 업로드 응답 DTO")
@Getter
@AllArgsConstructor
public class FileUploadResponse {
    
    @Schema(description = "업로드된 파일의 접근 가능한 URL", example = "https://yourdomain.com/uploads/example.jpg")
    private String fileUrl;
}
