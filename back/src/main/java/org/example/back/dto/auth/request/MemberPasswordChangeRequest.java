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
public class MemberPasswordChangeRequest {
    
    @Schema(description = "기존 비밀번호", example = "oldPassword123!")
    @NotBlank(message = "기존 비밀번호는 필수입니다.")
    private String oldPassword;
    
    @Schema(description = "새로운 비밀번호", example = "newPassword123!")
    @NotBlank(message = "새로운 비밀번호는 필수입니다.")
    private String newPassword;
}
