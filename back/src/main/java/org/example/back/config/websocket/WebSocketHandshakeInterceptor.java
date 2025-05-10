package org.example.back.config.websocket;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.security.JwtTokenProvider;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        
        // 연결 전 호출 (토큰 인증, 사용자 ID 추츨 등의 작업 수행)
        if (!(request instanceof ServletServerHttpRequest servletServerHttpRequest)) {
            log.warn("잘못된 요청 타입");
            return false;
        }
        
        log.info("WebSocket Handshake 요청");
        
        HttpServletRequest httpServletRequest = servletServerHttpRequest.getServletRequest();
        String token = httpServletRequest.getParameter("token");
        
        if (token == null || token.isBlank()) {
            log.warn("WebSocket 연결 시 토큰 누락");
            return false;
        }
        
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket 연결 시 토큰 유효성 검증 실패: {}", token);
            return false;
        }
        
        try {
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("JWT 토큰 유효성 검증 실패");
                return false;
            }
            
            Long memberId = jwtTokenProvider.getMemberId(token);
            String role = jwtTokenProvider.getRole(token);
            
            attributes.put("memberId", memberId);
            attributes.put("role", role);
            
            log.info("WebSocket 인증 완료, memberId: {}, role: {}", memberId, role);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket 토큰 파싱 실패: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        
        // 연결 후 호출 (로깅 등의 작업 수행)
        log.info("WebSocket Handshake 완료");
    }
}
