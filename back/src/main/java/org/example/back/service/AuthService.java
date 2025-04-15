package org.example.back.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.domain.auth.RefreshToken;
import org.example.back.domain.member.Member;
import org.example.back.dto.auth.response.MemberTokenResponse;
import org.example.back.exception.auth.AuthErrorCode;
import org.example.back.exception.auth.AuthException;
import org.example.back.repository.MemberRepository;
import org.example.back.repository.auth.RefreshTokenRepository;
import org.example.back.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    
    public MemberTokenResponse refreshAccessToken(String refreshToken) {
        log.debug("[Token Refresh] 시작 - 입력 RefreshToken= {}", refreshToken);
        
        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("유효하지 않는 RefreshToken 요청");
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        
        // 토큰에서 사용자 ID 추
        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        log.debug("[Token Refresh] 사용자 ID 추출 성공 memberId= {}", memberId);
        
        // 저장된 토큰 확인
        RefreshToken savedToken = refreshTokenRepository.findByMemberId(memberId).orElseThrow(() -> {
            log.warn("저장된 RefreshToken 이 존재하지 않음 memberId= {}", memberId);
            return new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        });
        
        if (!savedToken.getToken().equals(refreshToken)) {
            log.warn("저장된 RefreshToken 과 일치하지 않음 memberId= {}", memberId);
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }
        
        // Member 조회
        Member member = memberRepository.findById(memberId).orElseThrow(() -> {
            log.error("사용자 정보 없음 memberId= {}", memberId);
            return new AuthException(AuthErrorCode.MEMBER_NOT_FOUND);
        });
        
        // 새 AccessToken 생성 TODO, role 하드코딩
        String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), "USER");
        log.info("✅ AccessToken 재발급 성공 - memberId={}", memberId);
        
        return MemberTokenResponse.builder().accessToken(newAccessToken).refreshToken(refreshToken).build();
    }
}
