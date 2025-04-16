package org.example.back.service;

import org.example.back.domain.auth.RefreshToken;
import org.example.back.domain.member.Member;
import org.example.back.dto.auth.response.MemberTokenResponse;
import org.example.back.exception.auth.AuthErrorCode;
import org.example.back.exception.auth.AuthException;
import org.example.back.repository.MemberRepository;
import org.example.back.repository.auth.RefreshTokenRepository;
import org.example.back.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    
    @InjectMocks
    private AuthService authService;
    
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    private static final String VALID_REFRESH_TOKEN = "valid-refresh-token";
    private static final String INVALID_REFRESH_TOKEN = "invalid-refresh-token";
    
    private static final Long MEMBER_ID = 1L;
    
    private Member member;
    private RefreshToken refreshToken;
    
    @BeforeEach
    void 준비() {
        member = Member.builder().id(MEMBER_ID).username("testUser").password("password").nickname("testNick")
                .email("test@google.com").phone("010-1234-5678").build();
        
        refreshToken = RefreshToken.builder().id(MEMBER_ID).member(member).token(VALID_REFRESH_TOKEN)
                .expiresAt(LocalDateTime.now().plusDays(7)).build();
    }
    
    @Nested
    @DisplayName("AccessToken 재발급 - 성공")
    class Success {
        
        @Test
        @DisplayName("정상적인 RefreshToken -> AccessToken 재발급 성공")
        void refreshAccessToken_성공() {
            // when
            when(jwtTokenProvider.validateToken(VALID_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getMemberId(VALID_REFRESH_TOKEN)).thenReturn(MEMBER_ID);
            when(refreshTokenRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(refreshToken));
            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
            when(jwtTokenProvider.createAccessToken(MEMBER_ID, "USER")).thenReturn("new-access-token");
            
            // then
            MemberTokenResponse response = authService.refreshAccessToken(VALID_REFRESH_TOKEN);
            
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo(VALID_REFRESH_TOKEN);
        }
        
        @Test
        @DisplayName("정상적인 logout 성공")
        void logout_성공() {
            // given
            when(refreshTokenRepository.findByToken(VALID_REFRESH_TOKEN)).thenReturn(Optional.of(refreshToken));
            
            //when
            authService.logout(VALID_REFRESH_TOKEN);
            
            // then
            verify(refreshTokenRepository).delete(refreshToken);
        }
    }
    
    @Nested
    @DisplayName("AccessToken 재발급 - 실패")
    class Failure {
        
        @Test
        @DisplayName("토큰 유효성 검증 실패 -> INVALID_REFRESH_TOKEN 예외")
        void 토큰_유효하지않음() {
            when(jwtTokenProvider.validateToken(INVALID_REFRESH_TOKEN)).thenReturn(false);
            
            AuthException exception = assertThrows(AuthException.class,
                    () -> authService.refreshAccessToken(INVALID_REFRESH_TOKEN));
            
            assertThat(exception.getErrorcode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        
        @Test
        @DisplayName("RefreshToken 저장소에 없음 -> REFRESH_TOKEN_NOT_FOUND 예외")
        void 저장된_토큰없음() {
            when(jwtTokenProvider.validateToken(VALID_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getMemberId(VALID_REFRESH_TOKEN)).thenReturn(MEMBER_ID);
            when(refreshTokenRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());
            
            AuthException exception = assertThrows(AuthException.class,
                    () -> authService.refreshAccessToken(VALID_REFRESH_TOKEN));
            
            assertThat(exception.getErrorcode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        
        @Test
        @DisplayName("DB 에 저장된 토큰과 입력 토큰 불일치 -> REFRESH_TOKEN_MISMATCH 예외")
        void 토큰_불일치() {
            RefreshToken mismatchedToken = RefreshToken.builder().id(1L).member(member).token("other-token")
                    .expiresAt(LocalDateTime.now().plusDays(1)).build();
            
            when(jwtTokenProvider.validateToken(VALID_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getMemberId(VALID_REFRESH_TOKEN)).thenReturn(MEMBER_ID);
            when(refreshTokenRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(mismatchedToken));
            
            AuthException exception = assertThrows(AuthException.class,
                    () -> authService.refreshAccessToken(VALID_REFRESH_TOKEN));
            
            assertThat(exception.getErrorcode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }
        
        @Test
        @DisplayName("사용자 정보 없음 -> MEMBER_NOT_FOUND 예외")
        void 사용자없음() {
            when(jwtTokenProvider.validateToken(VALID_REFRESH_TOKEN)).thenReturn(true);
            when(jwtTokenProvider.getMemberId(VALID_REFRESH_TOKEN)).thenReturn(MEMBER_ID);
            when(refreshTokenRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(refreshToken));
            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
            
            AuthException exception = assertThrows(AuthException.class,
                    () -> authService.refreshAccessToken(VALID_REFRESH_TOKEN));
            
            assertThat(exception.getErrorcode()).isEqualTo(AuthErrorCode.MEMBER_NOT_FOUND);
        }
        
        @Test
        @DisplayName("RefreshToken 저장소에 없음 -> REFRESH_TOKEN_NOT_FOUND 예외")
        void 로그아웃_실패_저장된_토큰_없음() {
            // given
            when(refreshTokenRepository.findByToken(INVALID_REFRESH_TOKEN)).thenReturn(Optional.empty());
            
            // when
            AuthException exception = assertThrows(AuthException.class, () -> authService.logout(INVALID_REFRESH_TOKEN));
            
            // then
            assertThat(exception.getErrorcode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
            
        }
    }
}
