package org.example.back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.back.dto.member.request.MemberLoginRequest;
import org.example.back.dto.member.request.MemberPasswordChangeRequest;
import org.example.back.dto.member.request.MemberRegisterRequest;
import org.example.back.dto.member.response.MemberResponse;
import org.example.back.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "회원 관리 API", description = "회원가입, 로그인, 조회, 비밀번호 변경, 삭제")
public class MemberController {
    
    private final MemberService memberService;
    
    @PostMapping("/register")
    @Operation(summary = "회원 가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "409", description = "중복된 사용자 또는 닉네임"),
    })
    public ResponseEntity<MemberResponse> register(@Valid @RequestBody MemberRegisterRequest request) {
        return ResponseEntity.ok(memberService.registerMember(request));
    }
    
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 로그인 처리")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "비밀번호 불일치"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<MemberResponse> login(@Valid @RequestBody MemberLoginRequest request) {
        return ResponseEntity.ok(memberService.login(request));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "회원 정보 조회", description = "ID 기반으로 회원 정보를 가져온다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 정보 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<MemberResponse> getMember(
            @Parameter(description = "조회할 회원의 ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMemberById(id));
    }
    
    // 비밀번호 변경
    @PutMapping("/{id}/password")
    @Operation(summary = "비밀번호 변경", description = "기존 비밀번호를 확인 후 새로운 비밀번호로 변경한다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 정보 조회 성공"),
            @ApiResponse(responseCode = "400", description = "기존 비밀번호 불일치"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<String> changePassword(
            @Parameter(description = "비밀번호 변경할 회원 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody MemberPasswordChangeRequest request) {
        memberService.changePassword(id, request);
        return ResponseEntity.ok("비밀번호가 변경되었습니다.");
    }
    
    // 회원 삭제
    @DeleteMapping("/{id}")
    @Operation(summary = "회원 삭제", description = "사용자를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 정보 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<String> deleteMember(
            @Parameter(description = "삭제할 회원 ID", example = "1") @PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok("회원이 삭제되었습니다.");
    }
}
