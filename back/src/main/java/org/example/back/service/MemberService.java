package org.example.back.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.back.domain.member.Member;
import org.example.back.dto.member.request.MemberRequest;
import org.example.back.dto.member.response.MemberResponse;
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
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
    
    // 회원 가입
    @Transactional
    public MemberResponse registerMember(MemberRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 사용자 이름입니다.");
        }
        
        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        }
        
        Member member = Member.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();
        
        memberRepository.save(member);
        return new MemberResponse(member.getId(), member.getUsername(), member.getNickname());
    }
    
    // 로그인
    public MemberResponse login(MemberRequest request) {
        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        
        return new MemberResponse(member.getId(), member.getUsername(), member.getNickname());
    }
    
    // 회원 정보 조회
    public MemberResponse getMemberById(Long id) {
        Member member = findMemberById(id);
        
        return new MemberResponse(member.getId(), member.getUsername(), member.getNickname());
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
