package org.example.back.repository.auth;

import org.example.back.domain.auth.RefreshToken;
import org.example.back.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findToken(String token);
    
    Optional<RefreshToken> findByMember(Member member);
    
    void deleteByMember(Member member);
}
