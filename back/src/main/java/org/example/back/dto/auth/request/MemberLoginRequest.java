package org.example.back.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberLoginRequest {
    
    @Schema(description = "로그인 ID", example = "user123")
    @NotBlank(message = "아이디는 필수입니다.")
    private String username;
    
    @Schema(description = "비밀번호", example = "password123!")
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
