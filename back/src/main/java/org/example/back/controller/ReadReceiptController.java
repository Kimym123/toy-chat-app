package org.example.back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.dto.websocket.response.ReadReceiptResponse;
import org.example.back.service.message.ReadReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "읽음 상태 API", description = "채팅방 읽음 상태 및 안읽은 메시지 수 조회")
@Slf4j
@RestController
@RequestMapping("/api/chat/room")
@RequiredArgsConstructor
public class ReadReceiptController {
    
    private final ReadReceiptService readReceiptService;
    
    @Operation(
            summary = "채팅방 읽음 상태 목록 조회",
            description = "현재 채팅방에 참여 중인 사용자들의 마지막 읽은 메시지 정보를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/{chatRoomId}/read-status")
    public ResponseEntity<List<ReadReceiptResponse>> getReadStatuses(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal Long memberId
    ) {
        log.debug("[ReadStatus API] 채팅방 ID: {}, 회원 ID: {}", chatRoomId, memberId);
        
        List<ReadReceiptResponse> responseList = readReceiptService.getReadStatuses(chatRoomId, memberId);
        log.debug("[ReadStatus API] 응답 수: {}", responseList.size());
        
        return ResponseEntity.ok(responseList);
    }
    
    @Operation(
            summary = "채팅방 안읽은 메시지 수 조회",
            description = "해당 채팅방에서 사용자가 읽지 않은 메시지 수를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/{chatRoomId}/unread-count")
    public ResponseEntity<Integer> getUnreadMessageCount(
            @Parameter(description = "채팅방 ID", example = "101") @PathVariable Long chatRoomId,
            @AuthenticationPrincipal Long memberId
    ) {
        log.debug("[UnreadCount API] 채팅방 ID: {}, 회원 ID: {}", chatRoomId, memberId);
        
        int unreadCount = readReceiptService.getUnreadMessageCount(chatRoomId, memberId);
        log.debug("[UnreadCount API] 응답 수: {}", unreadCount);
        
        return ResponseEntity.ok(unreadCount);
    }
}
