package org.example.back.dto.member.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRequest {
    private Long id;
    private String username;
    private String password;    // 새 비밀번호
    private String oldPassword; // 기존 비밀번호 추가
    private String nickname;
    private String email;
    private String phone;
    
    // 로그인용 생성자 & 비밀번호 변경용
    public MemberRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    // 회원가입용 생성자
    
    public MemberRequest(String username, String password, String nickname, String email, String phone) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
    }
}
