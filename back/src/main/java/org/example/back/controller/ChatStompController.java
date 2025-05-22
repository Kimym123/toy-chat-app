package org.example.back.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.domain.message.ChatMessage;
import org.example.back.dto.message.request.ChatMessageRequest;
import org.example.back.dto.message.response.ChatMessageResponse;
import org.example.back.service.message.ChatMessageService;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {
    
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    
    // 클라이언트가 /pub/chat/send로 메시지를 보내면 실행.
    @MessageMapping("/chat/send")
    public void sendSystemMessageAndBroadcast(
            @Payload ChatMessageRequest request,
            @Header("simpSessionAttributes") Map<String, Object> attributes
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
    }
}
