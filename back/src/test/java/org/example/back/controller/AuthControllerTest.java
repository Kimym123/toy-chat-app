package org.example.back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.back.dto.auth.request.MemberTokenRefreshRequest;
import org.example.back.dto.auth.response.MemberTokenResponse;
import org.example.back.exception.auth.AuthErrorCode;
import org.example.back.exception.auth.AuthException;
import org.example.back.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@WithMockUser
public class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private AuthService authService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String URL = "/api/token/refresh";
    
    private ResultActions postJson(String url, Object body) throws Exception {
        return mockMvc.perform(post(url).with(csrf()) // CSRF 자동 처리
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }
    
    @Nested
    @DisplayName("AccessToken 재발급 API - 성공")
    class Success {
        
        @Test
        @DisplayName("정상적인 RefreshToken 요청 -> 새로운 AccessToken 반환")
        void refreshToken_성공() throws Exception {
            // given
            String refreshToken = "valid-refresh-token";
            MemberTokenRefreshRequest request = new MemberTokenRefreshRequest(refreshToken);
            
            MemberTokenResponse response = MemberTokenResponse.builder().accessToken("new-access-token")
                    .refreshToken(refreshToken).build();
            
            when(authService.refreshAccessToken(refreshToken)).thenReturn(response);
            
            // when & then
            postJson(URL, request).andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value(refreshToken));
        }
    }
    
    @Nested
    @DisplayName("AcessToken 재발급 API - 실패")
    class Failure {
        
        @Test
        @DisplayName("RefreshToken 미입력 -> 400 Bad Request")
        void refreshToken_빈값_잘못된_요청() throws Exception {
            // given
            MemberTokenRefreshRequest request = new MemberTokenRefreshRequest("");
            
            // when & then
            postJson(URL, request).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("RefreshToken 은 필수입니다.")));
        }
        
        @Test
        @DisplayName("유효하지 않은 RefreshToken -> 401 Unauthorized")
        void refreshToken_유효하지_않음() throws Exception {
            // given
            String invalidToken = "invalid-refresh-token";
            MemberTokenRefreshRequest request = new MemberTokenRefreshRequest(invalidToken);
            
            doThrow(new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN)).when(authService)
                    .refreshAccessToken(invalidToken);
            
            // when & then
            postJson(URL, request).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage()));
        }
        
        @Test
        @DisplayName("DB 에 저장된 RefreshToken 없음 -> 404 Not Found")
        void refreshToken_DB_없음() throws Exception {
            // given
            String notFoundToken = "not-found-refresh-token";
            MemberTokenRefreshRequest request = new MemberTokenRefreshRequest(notFoundToken);
            
            doThrow(new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND)).when(authService)
                    .refreshAccessToken(notFoundToken);
            
            // when & then
            postJson(URL, request).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage()));
        }
        
        @Test
        @DisplayName("RefreshToken 은 존재하나 사용자 없음 -> 404 Not Found")
        void refreshToken_사용자_없음() throws Exception {
            // given
            String token = "refresh-token-without-user";
            MemberTokenRefreshRequest request = new MemberTokenRefreshRequest(token);
            
            doThrow(new AuthException(AuthErrorCode.MEMBER_NOT_FOUND)).when(authService).refreshAccessToken(token);
            
            // when & then
            postJson(URL, request).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(AuthErrorCode.MEMBER_NOT_FOUND.getMessage()));
        }
    }
}
