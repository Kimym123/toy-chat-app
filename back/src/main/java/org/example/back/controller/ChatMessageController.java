package org.example.back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.back.dto.message.request.ChatMessageEditRequest;
import org.example.back.dto.message.response.ChatMessageResponse;
import org.example.back.service.message.ChatMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat Message API", description = "채팅 메시지 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/message")
public class ChatMessageController {
    
    private final ChatMessageService chatMessageService;
    
    @Operation(summary = "채팅 메시지 수정", description = "본인의 메시지를 5분 이내 수정 가능")
    @PutMapping("/{messageId}")
    public ResponseEntity<ChatMessageResponse> editMessage(
            @Parameter(description = "수정할 메시지 ID") @PathVariable Long messageId,
            @Valid @RequestBody ChatMessageEditRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        ChatMessageResponse response = chatMessageService.editMessage(memberId, messageId, request);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "메시지 삭제", description = "본인의 메시지를 소프트 삭제합니다.")
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @Parameter(description = "삭제할 메시지 ID") @PathVariable Long messageId,
            @AuthenticationPrincipal Long memberId
    ) {
        chatMessageService.deleteMessage(memberId, messageId);
        
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "메시지 삭제 취소", description = "본인이 삭제한 메시지를 5분 이내에 복구합니다.")
    @PostMapping("/{messageId}/restore")
    public ResponseEntity<Void> restoreMessage(
            @Parameter(description = "복구할 메시지 ID") @PathVariable Long messageId,
            @AuthenticationPrincipal Long memberId
    ) {
        chatMessageService.restoreMessage(memberId, messageId);
        
        return ResponseEntity.ok().build();
    }
}
