package org.example.back.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "보호된 API", description = "JWT 인증 후 접근 가능한 테스트 API")
@RestController
@RequestMapping("/api/test")
@Slf4j
public class ProtectedTestController {
    
    @GetMapping("/secure")
    public ResponseEntity<String> secure() {
        log.info("🔐 보호된 API 호출 성공");
        return ResponseEntity.ok("인증된 사용자만 접근 가능합니다.");
    }
}