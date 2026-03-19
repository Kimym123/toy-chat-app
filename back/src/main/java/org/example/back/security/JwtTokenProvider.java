package org.example.back.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidityInSeconds;

    @Getter
    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidityInSeconds;

    private SecretKey key;
    
    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);  // HS256용 SecretKey 반환
    }
    
    // 엑세스 토큰 생성
    public String createAccessToken(Long memberId, String role) {
        return createToken(memberId, role, accessTokenValidityInSeconds);
    }
    
    // 리프레시 토큰 생성 (role 없음)
    public String createRefreshToken(Long memberId) {
        return createToken(memberId, null, refreshTokenValidityInSeconds);
    }
    
    // 내부용 토큰 생성 메서드
    private String createToken(Long memberId, String role, long validityInSeconds) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(validityInSeconds);
        
        return Jwts.builder().subject(String.valueOf(memberId)).issuedAt(Date.from(now)).expiration(Date.from(expiry))
                .claim("role", role) // null 이면 무시됨
                .signWith(key, Jwts.SIG.HS256).compact();
        
    }
    
    public TokenInfo parseToken(String token) {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new TokenInfo(
                Long.valueOf(claims.getSubject()),
                (String) claims.get("role")
        );
    }

}
