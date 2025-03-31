package org.example.back.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.back.domain.member.Member;
import org.example.back.dto.member.request.MemberRequest;
import org.example.back.dto.member.response.MemberResponse;
import org.example.back.exception.member.MemberErrorCode;
import org.example.back.exception.member.MemberException;
import org.example.back.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    
    // 중복 코드 제거용
    private Member findMemberById(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new MemberException(MemberErrorCode.USER_NOT_FOUND));
    }
    
    // 회원 가입
    @Transactional
    public MemberResponse registerMember(MemberRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new MemberException(MemberErrorCode.DUPLICATE_USERNAME);
        }
        
        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new MemberException(MemberErrorCode.DUPLICATE_NICKNAME);
        }
        
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new MemberException(MemberErrorCode.DUPLICATE_EMAIL);
        }
        
        if (memberRepository.existsByPhone(request.getPhone())) {
            throw new MemberException(MemberErrorCode.DUPLICATE_PHONE);
        }
        
        Member member = Member.builder().username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword())).nickname(request.getNickname())
                .email(request.getEmail()).phone(request.getPhone()).build();
        
        memberRepository.save(member);
        return new MemberResponse(member.getId(), member.getUsername(), member.getNickname(), member.getEmail(),
                member.getPhone());
    }
    
    // 로그인
    public MemberResponse login(MemberRequest request) {
        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        
        return new MemberResponse(member.getId(), member.getUsername(), member.getNickname(), member.getEmail(),
                member.getPhone());
    }
    
    // 회원 정보 조회
    public MemberResponse getMemberById(Long id) {
        Member member = findMemberById(id);
        
        return new MemberResponse(member.getId(), member.getUsername(), member.getNickname(), member.getEmail(),
                member.getPhone());
    }
    
    // 비밀번호 변경
    @Transactional
    public void changePassword(Long id, MemberRequest request) {
        if (!id.equals(request.getId())) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }
        
        Member member = memberRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (!passwordEncoder.matches(request.getOldPassword(), member.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
        }
        
        member.setPassword(passwordEncoder.encode(request.getPassword()));
        memberRepository.save(member);
    }
    
    // 회원 탈퇴
    @Transactional
    public void deleteMember(Long id) {
        Member member = findMemberById(id);
        
        memberRepository.delete(member);
    }
}
