package org.example.back.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberTokenRefreshRequest {
    
    @NotBlank(message = "RefreshToken 은 필수입니다.")
    private String refreshToken;
}
