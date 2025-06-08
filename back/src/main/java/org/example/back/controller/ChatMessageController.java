package org.example.back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.back.dto.message.request.ChatMessageEditRequest;
import org.example.back.dto.message.response.ChatMessageResponse;
import org.example.back.service.message.ChatMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @PutMapping("/edit")
    public ResponseEntity<ChatMessageResponse> editMessage(
            @RequestBody ChatMessageEditRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        ChatMessageResponse response = chatMessageService.editMessage(memberId, request);
        
        return ResponseEntity.ok(response);
    }
}
