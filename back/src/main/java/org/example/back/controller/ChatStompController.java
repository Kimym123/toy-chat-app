package org.example.back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.example.back.dto.websocket.response.StompErrorResponse;
import org.example.back.exception.base.CustomException;
import org.example.back.exception.base.ErrorCode;
import org.example.back.exception.message.ChatMessageErrorCode;
import org.example.back.exception.message.ChatMessageException;
import org.example.back.service.message.ChatMessageService;
import org.example.back.service.message.ReadReceiptService;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Tag(name = "채팅 WebSocket", description = "STOMP 메시징 API")
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageService chatMessageService;
    private final ReadReceiptService readReceiptService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Operation(
            summary = "채팅 메시지 전송",
            description = "클라이언트가 /pub/chat/send로 채팅 메시지를 전송하면 해당 채팅방 (/sub/chat/room/{id})에 브로드캐스트합니다."
    )
    @MessageMapping("/chat/send")
    public void handleChatMessage(
            @Parameter(description = "채팅 메시지 요청 DTO") @Payload ChatMessageRequest request,
            @Parameter(description = "WebSocket 세션 정보") @Header("simpSessionAttributes") Map<String, Object> attributes
    ) {

        Long memberId = (Long) attributes.get("memberId");
        if (memberId == null) {
            log.warn("WebSocket 세션에 memberId 없음 - 메시지 전송 거부");
            throw new ChatMessageException(ChatMessageErrorCode.UNAUTHORIZED_ACCESS);
        }
        log.debug("[Send Message] 시작 - 입력 request={}, memberId={}", request, memberId);

        // 보안상 WebSocket 세션 기준으로 senderId 강제 설정
        request.setSenderId(memberId);

        // 메시지 보내기
        ChatMessage savedMessage = switch (request.getType()) {
            case TEXT -> chatMessageService.saveTextMessage(request);
            case IMAGE, FILE -> chatMessageService.saveFileMessage(request);
            default -> {
                log.warn("지원하지 않는 메시지 타입: {}", request.getType());
                throw new ChatMessageException(ChatMessageErrorCode.UNSUPPORTED_MESSAGE_TYPE);
            }
        };

        ChatMessageResponse response = ChatMessageResponse.from(savedMessage);

        // 메시지 전송
        simpMessagingTemplate.convertAndSend("/sub/chat/room/" + request.getChatRoomId(), response);
        log.debug("[Send Message] 브로드캐스트 완료 - messageId={}, roomId={}, clientMessageId={}",
                response.getMessageId(), response.getChatRoomId(), response.getClientMessageId());
    }

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
            throw new ChatMessageException(ChatMessageErrorCode.MEMBER_ID_MISMATCH);
        }

        // 읽음 상태 업데이트 처리
        readReceiptService.updateLastReadMessageId(request);
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

    @MessageExceptionHandler(CustomException.class)
    @SendToUser("/queue/errors")
    public StompErrorResponse handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("[STOMP][{}] {}", errorCode.name(), errorCode.getMessage());
        return StompErrorResponse.of(errorCode.name(), errorCode.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public StompErrorResponse handleException(Exception exception) {
        log.error("[STOMP][UNHANDLED] {}", exception.getMessage(), exception);
        return StompErrorResponse.of("INTERNAL_ERROR", "메시지 처리 중 오류가 발생했습니다.");
    }
}
