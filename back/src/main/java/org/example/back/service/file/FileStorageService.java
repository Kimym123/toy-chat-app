package org.example.back.service.file;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.example.back.domain.file.UploadedFile;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    // 파일을 디스크에 저장하고 메타데이터(UploadedFile)를 DB 에 기록 후 반환
    @Operation(summary = "파일 저장",
            description = "업로드된 파일을 디스크에 저장하고 메타데이터를 DB 에 기록한다. 저장된 UploadedFile 엔티티를 반환.")
    UploadedFile save(
            @Parameter(description = "업로드할 파일", required = true)
            MultipartFile file,
            Long memberId
    );
}
