package org.example.back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.example.back.dto.file.FileUploadResponse;
import org.example.back.service.file.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileUploadController {
    
    private final FileStorageService fileStorageService;
    
    @PostMapping("/upload")
    @Operation(summary = "파일 업로드", description = "멀티파트 파일을 업로드하고 접근 가능한 URL을 반환한다.")
    public ResponseEntity<FileUploadResponse> upload(
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        String savedUrl = fileStorageService.save(file);
        return ResponseEntity.ok(new FileUploadResponse(savedUrl));
    }
}
