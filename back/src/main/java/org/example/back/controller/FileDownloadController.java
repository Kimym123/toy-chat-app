package org.example.back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.back.service.file.FileDownloadService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "파일 다운로드 API", description = "업로드된 파일을 권한 검증 후 다운로드한다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileDownloadController {

    private final FileDownloadService fileDownloadService;

    @Operation(summary = "파일 다운로드",
            description = "fileId 로 파일을 다운로드한다. 권한: 업로더 본인 OR 해당 파일이 사용된 채팅방의 참여자.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "다운로드 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "다운로드 권한 없음"),
            @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
    })
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> download(
            @Parameter(description = "다운로드할 파일 ID", example = "42") @PathVariable Long fileId,
            @AuthenticationPrincipal Long memberId
    ) {
        return fileDownloadService.download(fileId, memberId);
    }
}
