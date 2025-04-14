package org.example.back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.back.config.SecurityConfig;
import org.example.back.dto.auth.response.TokenResponse;
import org.example.back.dto.auth.request.MemberPasswordChangeRequest;
import org.example.back.dto.auth.request.MemberRegisterRequest;
import org.example.back.dto.member.response.MemberResponse;
import org.example.back.exception.member.MemberException;
import org.example.back.security.JwtTokenProvider;
import org.example.back.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.example.back.exception.member.MemberErrorCode.USER_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class)
@WithMockUser
public class MemberControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private MemberService memberService;
    
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MemberRegisterRequest request;
    private MemberResponse response;
    
    @BeforeEach
    void 준비() {
        request = MemberRegisterRequest.builder().username("testUser").password("password").nickname("testNickName")
                .email("test@google.com").phone("010-1234-5678").build();
        response = MemberResponse.builder().id(1L).username("testUser").nickname("testNickName")
                .email("test@google.com").phone("010-1234-5678").build();
    }
    
    @Test
    @DisplayName("회원가입 성공")
    void 회원가입_성공() throws Exception {
        when(memberService.registerMember(any())).thenReturn(response);
        
        mockMvc.perform(post("/api/members/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L)).andExpect(jsonPath("$.username").value("testUser"))
                .andExpect(jsonPath("$.nickname").value("testNickName"));
    }
    
    @Test
    @DisplayName("로그인 성공")
    @WithAnonymousUser
    void 로그인_성공() throws Exception {
        // given
        TokenResponse tokenResponse = TokenResponse.builder().accessToken("test-access-token")
                .refreshToken("test-refresh-token").build();
        
        when(memberService.login(any())).thenReturn(tokenResponse);
        
        mockMvc.perform(post("/api/members/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"));
    }
    
    @Test
    @DisplayName("회원 조회 성공")
    void 회원조회_성공() throws Exception {
        when(memberService.getMemberById(1L)).thenReturn(response);
        
        mockMvc.perform(get("/api/members/{id}", 1L)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testUser"))
                .andExpect(jsonPath("$.nickname").value("testNickName"));
    }
    
    @Test
    @DisplayName("비밀번호 변경 성공")
    void 비밀번호_변경_성공() throws Exception {
        MemberPasswordChangeRequest changeRequest = MemberPasswordChangeRequest.builder().oldPassword("oldPassword")
                .newPassword("newPassword").build();
        
        // void 타입 -> doNothing()
        doNothing().when(memberService).changePassword(eq(1L), any());
        
        mockMvc.perform(put("/api/members/{id}/password", 1L).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest))).andExpect(status().isOk())
                .andExpect(content().string("비밀번호가 변경되었습니다."));
        
        verify(memberService, times(1)).changePassword(eq(1L), any());
    }
    
    @Test
    @DisplayName("회원 삭제 성공")
    void 회원삭제_성공() throws Exception {
        doNothing().when(memberService).deleteMember(1L);
        
        mockMvc.perform(delete("/api/members/{id}", 1L)).andExpect(status().isOk())
                .andExpect(content().string("회원이 삭제되었습니다."));
        
        verify(memberService, times(1)).deleteMember(1L);
    }
    
    // 예외처리기가 없어서 실패하는 상태
    @Test
    @DisplayName("회원 조회 실패 - 회원 없음")
    void 회원조회_실패() throws Exception {
        when(memberService.getMemberById(2L)).thenThrow(new MemberException(USER_NOT_FOUND));
        
        mockMvc.perform(get("/api/members/{id}", 2L)).andExpect(status().isNotFound());
    }
}
