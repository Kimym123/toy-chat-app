package org.example.back.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.example.back.domain.member.Member;
import org.example.back.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {
    
    @InjectMocks
    JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Mock
    JwtTokenProvider jwtTokenProvider;
    
    @Mock
    MemberRepository memberRepository;
    
    MockHttpServletRequest request;
    MockHttpServletResponse response;
    FilterChain filterChain;
    
    String token;
    Long memberId;
    Member member;
    
    @BeforeEach
    void 준비() {
        SecurityContextHolder.clearContext();
        
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        
        token = "test-access-token";
        memberId = 1L;
        
        member = Member.builder().id(memberId).username("testUser").password("password").nickname("testNickname")
                .email("test@example.com").phone("01012345678").build();
    }
    
    @Nested
    @DisplayName("인증 성공 케이스")
    class Success {
        
        @Test
        @DisplayName("정상 AccessToken 인증 성공")
        void 유효한토큰_인증성공() throws ServletException, IOException {
            // given
            request.addHeader("Authorization", "Bearer " + token);
            when(jwtTokenProvider.validateToken(token)).thenReturn(true);
            when(jwtTokenProvider.getMemberId(token)).thenReturn(memberId);
            when(jwtTokenProvider.getRole(token)).thenReturn("USER");
            
            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
            
            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isEqualTo(memberId);
            assertThat(authentication.getAuthorities()).extracting("authority").contains("ROLE_USER");
        }
        
        @Test
        @DisplayName("이미 인증된 SecurityConext 가 있는 경우 중복 인증되지 않음")
        void 이미인증됨_중복인증안함() throws ServletException, IOException {
            // given
            request.addHeader("Authorization", "Bearer " + token);
            when(jwtTokenProvider.validateToken(token)).thenReturn(true);
            when(jwtTokenProvider.getMemberId(token)).thenReturn(memberId);
            when(jwtTokenProvider.getRole(token)).thenReturn("USER");
            
            // 이미 인증된 상태 세팅
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("existingUser", null)
            );
            
            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
            
            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication.getPrincipal()).isEqualTo("existingUser"); // 기존 인증 유지
        }
    }
    
    @Nested
    @DisplayName("인증 실패 케이스")
    class Failure {
        @Test
        @DisplayName("Authorization 헤더가 없을 때 인증되지 않음")
        void 헤더없음_인증실패() throws ServletException, IOException {
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
        
        @Test
        @DisplayName("Authorization 헤더 형식이 잘못되었을 때 인증되지 않음")
        void 형식잘못됨_인증실패() throws ServletException, IOException {
            request.addHeader("Authorization", "Invalid " + token);
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
        
        @Test
        @DisplayName("토큰이 유효하지 않을 때 인증되지 않음")
        void 토큰무효_인증실패() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + token);
            when(jwtTokenProvider.validateToken(token)).thenReturn(false);
            
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
        
        @Test
        @DisplayName("Authorization 헤더에 'Bearer ' 는 있지만 토큰이 없음")
        void 토큰없음_인증실패() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer ");
            jwtAuthenticationFilter.doFilterInternal(request,response,filterChain);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
        
        @Test
        @DisplayName("Authorization 헤더에 'Bearer' 이 붙어서 잘못 전송")
        void Bearer_입력오류_인증실패() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer" + token); // Bearer 에 공백 없는 상태
            jwtAuthenticationFilter.doFilterInternal(request,response,filterChain);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
        
        @Test
        @DisplayName("JwtTokenProvider 내부 예외 발생 시 인증 안됨")
        void 내부예외발생_인증실패() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + token);
            when(jwtTokenProvider.validateToken(token)).thenThrow(new RuntimeException("Test Exception"));
            
            jwtAuthenticationFilter.doFilterInternal(request,response,filterChain);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}
