package org.example.back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.back.dto.room.request.CreateGroupChatRoomRequest;
import org.example.back.dto.room.request.CreatePrivateChatRoomRequest;
import org.example.back.dto.room.request.InviteChatRoomRequest;
import org.example.back.dto.room.request.LeaveChatRoomRequest;
import org.example.back.dto.room.response.ChatRoomResponse;
import org.example.back.service.room.ChatRoomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat Room API", description = "채팅방 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {
    
    private final ChatRoomService chatRoomService;
    
    @Operation(summary = "1:1 채팅방 생성", description = "상대방과의 1:1 채팅방을 생성합니다. 이미 존재하면 기존 채팅방을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "채팅방 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "상대방 회원을 찾을 수 없음")
    })
    @PostMapping("/private")
    public ResponseEntity<ChatRoomResponse> createPrivateChatRoom(
            @Valid @RequestBody CreatePrivateChatRoomRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        // 보안: 인증된 사용자 ID로 요청자 ID 덮어쓰기
        CreatePrivateChatRoomRequest securedRequest = CreatePrivateChatRoomRequest.builder()
                .memberId(memberId)
                .targetMemberId(request.getTargetMemberId())
                .build();
        
        ChatRoomResponse response = chatRoomService.createPrivateChatRoom(securedRequest);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "그룹 채팅방 생성", description = "그룹 채팅방을 생성합니다. 요청자는 자동으로 참여자에 포함됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "채팅방 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이름 누락 또는 유효하지 않은 회원 ID)")
    })
    @PostMapping("/group")
    public ResponseEntity<ChatRoomResponse> createGroupChatRoom(
            @Valid @RequestBody CreateGroupChatRoomRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        ChatRoomResponse response = chatRoomService.createGroupChatRoom(memberId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "내 채팅방 목록 조회", description = "현재 로그인한 사용자가 참여 중인 채팅방 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/my")
    public ResponseEntity<Page<ChatRoomResponse>> getMyChatRooms(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ChatRoomResponse> response = chatRoomService.getMyChatRooms(memberId, pageable);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "채팅방에 멤버 초대", description = "그룹 채팅방에 새로운 멤버들을 초대합니다. 1:1 채팅방에는 초대할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초대 성공"),
            @ApiResponse(responseCode = "400", description = "1:1 채팅방에는 초대 불가"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @PostMapping("/{roomId}/invite")
    public ResponseEntity<Void> inviteMembers(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Valid @RequestBody InviteChatRoomRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        chatRoomService.inviteMembers(roomId, request);
        
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "채팅방 나가기", description = "현재 로그인한 사용자가 채팅방에서 나갑니다. 마지막 참여자가 나가면 채팅방이 삭제됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "나가기 성공"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveChatRoom(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal Long memberId
    ) {
        // 보안: 인증된 사용자 ID로 요청 생성
        LeaveChatRoomRequest request = LeaveChatRoomRequest.builder()
                        .memberId(memberId)
                        .build();
        
        chatRoomService.leaveChatRoom(roomId, request);
        
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "채팅방 삭제", description = "채팅방을 소프트 삭제합니다. 참여자만 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "이미 삭제된 채팅방 또는 참여자가 아님"),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteChatRoom(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal Long memberId
    ) {
        chatRoomService.softDeleteChatRoom(roomId, memberId);
        
        return ResponseEntity.noContent().build();
    }
}
