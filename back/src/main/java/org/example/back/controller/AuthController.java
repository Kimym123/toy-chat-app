package org.example.back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.dto.auth.request.MemberLogoutRequest;
import org.example.back.dto.auth.request.MemberTokenRefreshRequest;
import org.example.back.dto.auth.response.TokenResponse;
import org.example.back.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증 API", description = "AccessToken 재발급, 로그아웃 등 인증 관련 기능 제공.")
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) return "***";
        return token.substring(0, 8) + "...";
    }

    @Operation(summary = "AccessToken 재발급",
            description = "저장된 RefreshToken 을 기반으로 새로운 AccessToken 을 발급. " + "RefreshToken 이 유효하지 않으면 예외 발생.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AccessToken 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "RefreshToken 이 유효하지 않거나 일치하지 않음"),
            @ApiResponse(responseCode = "404", description = "저장된 토큰 또는 사용자 정보 없음")
    })
    @PostMapping("/token/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody MemberTokenRefreshRequest request) {
        log.info("[Token Refresh 요청] refreshToken= {}", maskToken(request.getRefreshToken()));

        TokenResponse response = authService.refreshAccessToken(request.getRefreshToken());

        log.info("[Token Refresh 완료] accessToken 발급 완료 for refreshToken= {}", maskToken(request.getRefreshToken()));
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "로그아웃", description = "RefreshToken 을 삭제하여 로그아웃 처리")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "404", description = "해당 토큰을 가진 사용자가 존재하지 않음")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid MemberLogoutRequest request) {
        log.info("[로그아웃 요청] RefreshToken: {}", maskToken(request.getRefreshToken()));
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
