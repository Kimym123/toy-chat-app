package org.example.back.domain.member;

import jakarta.persistence.*;
import lombok.*;
import org.example.back.domain.base.BaseTimeEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member extends BaseTimeEntity implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username; // 회원 ID
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false, unique = true)
    private String nickname;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(unique = true)
    private String phone;
    
    @Column
    private String profileImageUrl;
    
    @Enumerated(EnumType.STRING)
    @Column
    @Builder.Default
    private MemberRole role = MemberRole.USER;
    
    public Member(String username, String password, String nickname, String email, String phone) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        this.role = MemberRole.USER; // 기본값 설정
    }
    
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    /**
     * 사용자 역할 변경
     *
     * @param newRole 새로운 역할
     */
    public void updateRole(MemberRole newRole) {
        this.role = newRole;
    }
    
    /**
     * 관리자급 권한인지 확인
     * 
     * @return 관리자급 권한 여부 (MODERATOR 이상)
     */
    public boolean isStaff() {
        return this.role.isStaff();
    }
    
    /**
     * 다른 사용자를 관리할 수 있는지 확인
     * 
     * @param targetMember 대상 사용자
     * @return 관리 권한 여부
     */
    public boolean canManage(Member targetMember) {
        return this.role.canManage(targetMember.role);
    }
    
    /**
     * 엔티티 저장 전 실행되는 메서드
     * - role이 null인 경우 기본값 USER로 설정
     */
    @PrePersist
    @PreUpdate
    public void setDefaultRole() {
        if (this.role == null) {
            this.role = MemberRole.USER;
        }
    }
    
    // UserDetails 인터페이스 구현
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security는 "ROLE_" 접두사를 기대합니다
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }
    
    @Override
    public String getUsername() {
        return this.username;
    }
    
    @Override
    public String getPassword() {
        return this.password;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 기능 미사용
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠금 기능 미사용 (추후 차단 기능 구현 시 수정)
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비밀번호 만료 기능 미사용
    }
    
    @Override
    public boolean isEnabled() {
        return true; // 계정 활성화 상태 (추후 탈퇴 기능 구현 시 수정)
    }
}
