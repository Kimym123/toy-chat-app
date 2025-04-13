package org.example.back.domain.auth;

import jakarta.persistence.*;
import lombok.*;
import org.example.back.domain.base.BaseTimeEntity;
import org.example.back.domain.member.Member;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
public class RefreshToken extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    @Column(nullable = false, unique = true, length = 512)
    private String token;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    public void update(String newToken, LocalDateTime newExpiresAt) {
        this.token = newToken;
        this.expiresAt = newExpiresAt;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
