package org.example.back.dto.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberTokenResponse {
    
    @Schema(description = "재발급된 AccessToken", example = "eyJhbGciOiJIUzI1...")
    private final String accessToken;
    
    @Schema(description = "요청한 RefreshToken (변경 없음)", example = "eyJhbGciOiJIUzI1...")
    private final String refreshToken;
}
