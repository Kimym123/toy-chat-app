package org.example.back.controller;

import lombok.RequiredArgsConstructor;
import org.example.back.dto.member.request.MemberRequest;
import org.example.back.dto.member.response.MemberResponse;
import org.example.back.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    
    private final MemberService memberService;
    
    // 회원 가입
    @PostMapping("/register")
    public ResponseEntity<MemberResponse> register(
            @RequestBody MemberRequest request) {
        return ResponseEntity.ok(memberService.registerMember(request));
    }
    
    // 로그인
    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(@RequestBody MemberRequest request) {
        return ResponseEntity.ok(memberService.login(request));
    }
    
    // 회원 정보 조회
    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMemberById(id));
    }
    
    // 비밀번호 변경
    @PutMapping("/{id}/password")
    public ResponseEntity<String> changePassword(
            @PathVariable Long id, @RequestBody MemberRequest request
    ) {
        memberService.changePassword(id, request);
        return ResponseEntity.ok("비밀번호가 변경되었습니다.");
    }
    
    // 회원 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok("회원이 삭제되었습니다.");
    }
}
