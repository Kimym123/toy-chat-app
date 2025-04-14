package org.example.back.dto.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TokenResponse {
    
    @Schema(description = "Access Token (30 분 유효)", example = "eyJhbGciOiJIUzI1...")
    private String accessToken;
    
    @Schema(description = "Refresh Token (14 일 유효)", example = "eyJhbGciOiJIUzI1...")
    private String refreshToken;
}
