package org.example.back.dto.member.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRegisterRequest {
    
    @Schema(description = "로그인 ID", example = "user123")
    @NotBlank(message = "아이디는 필수입니다.")
    private String username;
    
    @Schema(description = "비밀번호", example = "password123!")
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
    
    @Schema(description = "닉네임", example = "홍길동")
    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;
    
    @Schema(description = "이메일", example = "test@google.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    private String email;
    
    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;
}
