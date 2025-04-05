package org.example.back.service;

import org.example.back.domain.member.Member;
import org.example.back.dto.member.request.MemberLoginRequest;
import org.example.back.dto.member.request.MemberPasswordChangeRequest;
import org.example.back.dto.member.request.MemberRegisterRequest;
import org.example.back.dto.member.response.MemberResponse;
import org.example.back.exception.member.MemberException;
import org.example.back.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.example.back.exception.member.MemberErrorCode.USER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
 * MemberService 테스트 클래스.
 * 회원가입, 로그인, 회원 삭제에 대한 유닛 테스트.
 * */
@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {
    
    @InjectMocks
    private MemberService memberService;
    
    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    private Member member;
    
    @BeforeEach
    void 준비() {
        member = Member.builder().username("testUser").password("password").nickname("testNick")
                .email("test@google.com").phone("010-1234-5678").build();
    }
    
    private MemberRegisterRequest createMemberRequest(Member member) {
        return new MemberRegisterRequest(member.getUsername(), member.getPassword(), member.getNickname(),
                member.getEmail(), member.getPhone());
    }
    
    @Test
    @DisplayName("회원가입 성공 테스트 - 정상적으로 회원가입이 이뤄지는지 검증")
    void 회원가입_성공() {
        // given : 요청 정보 설정
        MemberRegisterRequest request = createMemberRequest(member);
        
        // when : 중복 체크 및 저장 설정
        when(memberRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(memberRepository.existsByNickname(request.getNickname())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(memberRepository.save(any(Member.class))).thenReturn(member);
        
        // then : 회원가입 실행 및 검증
        MemberResponse savedMember = memberService.registerMember(request);
        
        assertNotNull(savedMember);
        assertEquals("testUser", savedMember.getUsername());
        assertEquals("testNick", savedMember.getNickname());
        
        // encode, save 메서드가 호출되었는지 검증
        verify(passwordEncoder).encode(anyString());
        verify(memberRepository).save(any(Member.class));
    }
    
    @Test
    @DisplayName("회원가입 중복 테스트 - 중복된 사용자 이름이 존재할 경우 예외 확인")
    void 회원가입_증복_사용자명() {
        MemberRegisterRequest request = createMemberRequest(member);
        when(memberRepository.existsByUsername(request.getUsername())).thenReturn(true);
        
        Exception exception = assertThrows(MemberException.class, () -> memberService.registerMember(request));
        assertEquals("이미 존재하는 사용자 이름입니다.", exception.getMessage());
    }
    
    @Test
    @DisplayName("닉네임 중복 테스트 - 중복된 닉네임이 존재할 경우 예외 확인")
    void 회원가입_중복_닉네임() {
        MemberRegisterRequest request = createMemberRequest(member);
        when(memberRepository.existsByNickname(request.getNickname())).thenReturn(true);
        
        Exception exception = assertThrows(MemberException.class, () -> memberService.registerMember(request));
        assertEquals("이미 존재하는 닉네임입니다.", exception.getMessage());
    }
    
    @Test
    @DisplayName("로그인 성공 테스트")
    void 로그인_성공() {
        // given
        when(memberRepository.findByUsername(member.getUsername())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        
        // when
        MemberResponse response = memberService.login(new MemberLoginRequest(member.getUsername(), "password"));
        
        // then
        assertNotNull(response);
        assertEquals(member.getUsername(), response.getUsername());
    }
    
    @Test
    @DisplayName("로그인 실패 테스트 - 비밀번호 불일치")
    void 로그인_실패_비밀번호불일치() {
        // given
        when(memberRepository.findByUsername(member.getUsername())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        
        // then
        Exception exception = assertThrows(MemberException.class,
                () -> memberService.login(new MemberLoginRequest(member.getUsername(), "password")));
        assertEquals("비밀번호가 일치하지 않습니다.", exception.getMessage());
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 로그인 테스트")
    void 잘못된_로그인_요청() {
        // given
        when(memberRepository.findByUsername("nonExistentUser")).thenReturn(Optional.empty());
        
        // then
        Exception exception = assertThrows(MemberException.class,
                () -> memberService.login(new MemberLoginRequest("nonExistentUser", "password")));
        assertEquals("사용자를 찾을 수 없습니다.", exception.getMessage());
    }
    
    @Test
    @DisplayName("비밀번호 변경 성공 테스트")
    void 비밀번호_변경_성공() {
        // given
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        
        MemberPasswordChangeRequest request = MemberPasswordChangeRequest.builder().oldPassword("password")
                .newPassword("newPassword").build();
        
        // when
        memberService.changePassword(member.getId(), request);
        
        // then
        verify(memberRepository).save(any(Member.class));
    }
    
    @Test
    @DisplayName("비밀번호 변경 시 ID 불일치 예외 테스트")
    void 비밀번호_변경_실패_ID_불일치_예외() {
        // given
        MemberPasswordChangeRequest request = MemberPasswordChangeRequest.builder().oldPassword("password")
                .newPassword("newPassword").build();
        
        // when & then
        Exception exception = assertThrows(MemberException.class,
                () -> memberService.changePassword(2L, request));
        assertEquals(USER_NOT_FOUND.getMessage(), exception.getMessage());
    }
    
    @Test
    @DisplayName("ID 기반 회원 조회 테스트")
    void ID_기반_회원_조회() {
        // given
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        
        // when
        MemberResponse response = memberService.getMemberById(member.getId());
        
        // that
        assertNotNull(response);
        assertEquals(response.getId(), member.getId());
    }
    
    @Test
    @DisplayName("회원 삭제 테스트")
    void 회원_삭제() {
        // given
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        
        // when
        memberService.deleteMember(member.getId());
        
        // then
        verify(memberRepository).delete(member);
    }
}
