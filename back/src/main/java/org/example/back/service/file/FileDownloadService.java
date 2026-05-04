package org.example.back.service.file;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface FileDownloadService {

    @Operation(summary = "파일 다운로드",
            description = "fileId 로 파일을 조회하고 권한 검증 후 디스크에서 읽어 응답한다. " +
                    "권한: 업로더 본인 OR 해당 파일이 사용된 채팅방의 참여자.")
    ResponseEntity<Resource> download(
            @Parameter(description = "다운로드할 파일 ID") Long fileId,
            @Parameter(description = "요청자 ID (JWT 인증)") Long memberId
    );
}
