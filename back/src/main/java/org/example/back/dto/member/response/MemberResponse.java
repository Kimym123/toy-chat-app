package org.example.back.dto.member.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {
    
    @Schema(description = "회원 고유 번호", example = "1")
    private Long id;
    
    @Schema(description = "로그인 ID", example = "user123")
    private String username;
    
    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;
    
    @Schema(description = "이메일", example = "test@google.com")
    private String email;
    
    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;
}
