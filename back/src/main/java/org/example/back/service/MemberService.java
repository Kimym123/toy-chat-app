package org.example.back.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.back.domain.auth.RefreshToken;
import org.example.back.domain.member.Member;
import org.example.back.dto.auth.TokenResponse;
import org.example.back.dto.member.request.MemberLoginRequest;
import org.example.back.dto.member.request.MemberPasswordChangeRequest;
import org.example.back.dto.member.request.MemberRegisterRequest;
import org.example.back.dto.member.response.MemberResponse;
import org.example.back.exception.member.MemberException;
import org.example.back.repository.MemberRepository;
import org.example.back.repository.auth.RefreshTokenRepository;
import org.example.back.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.example.back.exception.member.MemberErrorCode.*;

@Service
@RequiredArgsConstructor
public class MemberService {
    
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    
    // 중복 코드 제거용
    private Member findMemberById(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new MemberException(USER_NOT_FOUND));
    }
    
    // 회원 가입
    @Transactional
    public MemberResponse registerMember(MemberRegisterRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new MemberException(DUPLICATE_USERNAME);
        }
        
        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new MemberException(DUPLICATE_NICKNAME);
        }
        
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new MemberException(DUPLICATE_EMAIL);
        }
        
        if (memberRepository.existsByPhone(request.getPhone())) {
            throw new MemberException(DUPLICATE_PHONE);
        }
        
        Member member = Member.builder().username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword())).nickname(request.getNickname())
                .email(request.getEmail()).phone(request.getPhone()).build();
        
        memberRepository.save(member);
        return new MemberResponse(member.getId(), member.getUsername(), member.getNickname(), member.getEmail(),
                member.getPhone());
    }
    
    // 로그인
    public TokenResponse login(MemberLoginRequest request) {
        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new MemberException(USER_NOT_FOUND));
        
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new MemberException(INVALID_PASSWORD);
        }
        
        // 토큰 발급
        // TODO 지금은 USER 하드코딩 -> 추후 리팩토링
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), "USER");
        String refreshTokenStr = jwtTokenProvider.createRefreshToken(member.getId());
        
        // 만료 시간 계산
        long validity = jwtTokenProvider.getRefreshTokenValidityInSeconds();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(validity);
        
        // 기존 토큰이 있는지 조회
        Optional<RefreshToken> existing = refreshTokenRepository.findByMember(member);
        
        if (existing.isPresent()) {
            // 있으므로 갱신
            existing.get().update(refreshTokenStr, expiresAt);
        } else {
            // 없으므로 새로 생성
            RefreshToken refreshToken = RefreshToken.builder().member(member).token(refreshTokenStr)
                    .expiresAt(expiresAt).build();
            
            refreshTokenRepository.save(refreshToken);
        }
        
        return TokenResponse.builder().accessToken(accessToken).refreshToken(refreshTokenStr).build();
    }
    
    // 회원 정보 조회
    public MemberResponse getMemberById(Long id) {
        Member member = findMemberById(id);
        
        return new MemberResponse(member.getId(), member.getUsername(), member.getNickname(), member.getEmail(),
                member.getPhone());
    }
    
    // 비밀번호 변경
    @Transactional
    public void changePassword(Long id, MemberPasswordChangeRequest request) {
        
        Member member = memberRepository.findById(id).orElseThrow(() -> new MemberException(USER_NOT_FOUND));
        
        if (!passwordEncoder.matches(request.getOldPassword(), member.getPassword())) {
            throw new MemberException(INVALID_CURRENT_PASSWORD);
        }
        
        member.setPassword(passwordEncoder.encode(request.getNewPassword()));
        memberRepository.save(member);
    }
    
    // 회원 탈퇴
    @Transactional
    public void deleteMember(Long id) {
        Member member = findMemberById(id);
        
        memberRepository.delete(member);
    }
}
