package org.example.back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.domain.message.ChatMessage;
import org.example.back.domain.message.TypingStatus;
import org.example.back.dto.message.request.ChatMessageRequest;
import org.example.back.dto.message.request.TypingStatusRequest;
import org.example.back.dto.message.response.ChatMessageResponse;
import org.example.back.dto.websocket.request.ReadMessageRequest;
import org.example.back.dto.websocket.response.ReadReceiptResponse;
import org.example.back.service.message.ChatMessageService;
import org.example.back.service.message.ReadReceiptService;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "채팅 WebSocket", description = "STOMP 메시징 및 읽음 처리 API")
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {
    
    private final ChatMessageService chatMessageService;
    private final ReadReceiptService readReceiptService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    
    // 채팅 메시지를 전송하고, 해당 채팅방에 브로드캐스트.
    @Operation(
            summary = "채팅 메시지 전송",
            description = "클라이언트가 /pub/chat/send로 채팅 메시지를 전송하면 해당 채팅방 (/sub/chat/room/{id})에 브로드캐스트합니다."
    )
    @MessageMapping("/chat/send")
    public void sendSystemMessageAndBroadcast(
            @Parameter(description = "채팅 메시지 요청 DTO") @Payload ChatMessageRequest request,
            @Parameter(description = "WebSocket 세션 정보") @Header("simpSessionAttributes") Map<String, Object> attributes
    ) {
        
        Long memberId = (Long) attributes.get("memberId");
        log.debug("[Send Message] 시작 - 입력 request={}, memberId={}", request, memberId);
        
        // 메시지 보내기
        ChatMessage savedMessage = switch (request.getType()) {
            case TEXT -> chatMessageService.saveTextMessage(request);
            case IMAGE, FILE -> chatMessageService.saveFileMessage(request);
            default -> {
                log.warn("지원하지 않는 메시지 타입: {}", request.getType());
                throw new IllegalArgumentException("지원하지 않는 타입");
            }
        };
        
        ChatMessageResponse response = ChatMessageResponse.from(savedMessage);
        
        // 메시지 전송
        simpMessagingTemplate.convertAndSend("/sub/chat/room/" + request.getChatRoomId(), response);
        log.debug("[Send Message] 저장 및 브로드캐스트 완료 - messageId={}, roomId={}",
                response.getMessageId(), response.getChatRoomId());
    }
    
    // 사용자의 읽음 메시지 ID를 업데이트하고, 다른 참여자에게 읽음 알림을 보냄.
    @Operation(
            summary = "메시지 읽음 처리",
            description = "클라이언트가 /pub/chat/read로 읽음 메시지를 전송하면, 해당 채팅방에 읽음 알림을 브로드캐스트합니다."
    )
    @MessageMapping("/chat/read")
    public void handleReadMessage(
            @Parameter(description = "읽음 처리 요청 DTO") @Payload ReadMessageRequest request,
            @Parameter(description = "WebSocket 세션 정보") @Header("simpSessionAttributes") Map<String, Object> attributes
    ) {
        Long memberId = (Long) attributes.get("memberId");
        log.debug("[Read Message] 시작 - 입력 request= {}, memberId={}", request, memberId);
        
        if (!Objects.equals(memberId, request.getMemberId())) {
            log.warn("읽음 알림 처리 실패 - memberId가 일치하지 않습니다.");
            throw new IllegalArgumentException("memberId가 일치하지 않습니다.");
        }
        
        // 읽음 상태 업데이트 처리
        readReceiptService.updateLastReadMessageId(request);
    }
    
    // 채팅방에 참여 중인 사용자들의 읽음 상태를 반환.
    @Operation(
            summary = "채팅방 읽음 상태 목록 조회",
            description = "현재 채팅방에 참여 중인 사용자들의 마지막 읽은 메시지 정보를 반환합니다."
    )
    @GetMapping("/api/chat/room/{chatRoomId}/read-status")
    public List<ReadReceiptResponse> getReadStatuses(@PathVariable Long chatRoomId) {
        log.debug("[ReadStatus API] 채팅방 ID: {}", chatRoomId);
        
        List<ReadReceiptResponse> responseList = readReceiptService.getReadStatuses(chatRoomId);
        log.debug("[ReadStatus API] 응답 수: {}", responseList.size());
        
        return responseList;
    }
    
    @Operation(
            summary = "채팅방 안읽은 메시지 수 조회",
            description = "해당 채팅방에서 사용자가 읽지 않은 메시지 수를 반환합니다."
    )
    @GetMapping("/api/chat/room/{chatRoomId}/unread-count/{memberId}")
    public int getUnreadMessageCount(
            @Parameter(description = "채팅방 ID", example = "101") @PathVariable Long chatRoomId,
            @Parameter(description = "회원 ID", example = "1") @PathVariable Long memberId
    ) {
        log.debug("[UnreadCount API] 채팅방 ID: {}, 회원 ID: {}", chatRoomId, memberId);
        
        int unreadCount = readReceiptService.getUnreadMessageCount(chatRoomId, memberId);
        log.debug("[UnreadCount API] 응답 수: {}", unreadCount);
        
        return unreadCount;
    }
    
    @Operation(
            summary = "실시간 타이핑 상태 전송",
            description = "사용자의 타이핑 상태를 브로드캐스트합니다."
    )
    @MessageMapping("/chat/typing")
    public void handleTyping(
            @Parameter(description = "사용자 타이핑 요청 DTO") @Payload TypingStatusRequest request,
            @Parameter(description = "WebSocket 세션 정보") @Header("simpSessionAttributes") Map<String, Object> attributes
    ) {
        Long memberId = (Long) attributes.get("memberId");
        
        if (memberId == null) {
            log.warn("WebSocket 세션에 memberId 없음 - 타이핑 알림 무시됨");
            return;
        }
        
        TypingStatus status = request.getTypingStatusEnum();
        
        readReceiptService.broadcastTypingStatus(request.getChatRoomId(), memberId, status);
    }
}
