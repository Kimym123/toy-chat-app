package org.example.back.service.file;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    
    // 파일을 저장하고 접근 가능한 URL 경로 반환
    @Operation(summary = "파일 저장", description = "업로드된 파일을 저장하고 접근 가능한 파일 URL을 반환한다.")
    String save(
            @Parameter(description = "업로드할 파일", required = true)
            MultipartFile file
    );
}
