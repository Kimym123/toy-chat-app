package org.example.back.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.exception.auth.AuthException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    /*
    * 요청마다 실행되는 인증 필터 (Spring Security 의 OncePerRequestFilter 상속)
    * */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 요청 헤더에서 JWT 토큰 추출
            String token = resolveToken(request);
            
            // 토큰이 존재하고 유효한 경우의 분기
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // 토큰에서 사용자 ID 와 권한 추출
                Long memberId = jwtTokenProvider.getMemberId(token);
                String role = jwtTokenProvider.getRole(token);
                
                // 이미 인증된 사용자가 아닐 경우 분기 (중복 인증 방지)
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 인증 객체 생성 (UserDetails 없이 memberId, role 기반)
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            memberId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    
                    // 현재 요청에 대한 인증 정보를 SecurityContext 에 저장
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("인증 성공, memberId: {}, role: {}", memberId, role);
                }
            }
        } catch (AuthException error) {
            log.warn("JWT 인증 실패: {}", error.getErrorCode().getMessage());
        } catch (Exception error) {
            log.error("필터 처리 중 알 수 없는 오류 발생", error);
        }
        
        // 다음 필터로 요청 전달 (필터 체인 유지)
        filterChain.doFilter(request, response);
    }
    
    /*
    * HTTP 요청 헤더에 Authorization 값을 추출하고 Bearer 제거
    * */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        
        if (bearer != null && bearer.startsWith("Bearer ")) {
            // "Bearer " 이후 토큰 값만 반환
            return bearer.substring(7);
        }
        
        return null;
    }
}
