package org.example.back.handler;

import static org.example.back.util.websocket.WebSocketUtils.getMemberId;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.domain.message.ChatMessage;
import org.example.back.dto.message.request.ChatMessageRequest;
import org.example.back.dto.message.response.ChatMessageResponse;
import org.example.back.repository.MemberRepository;
import org.example.back.service.message.ChatMessageService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final ObjectMapper objectMapper;
    private final ChatMessageService chatMessageService;
    
    // 채팅방 ID 과 연결된 WebSocketSession 리스트 (브로드캐스트용)
    private static final Map<Long, List<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final MemberRepository memberRepository;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        
        // 클라이언트 WebSocket 연결 성공 시 호출
        log.info("WebSocket 연결됨: {}", session.getId());
        
        Long memberId = getMemberId(session);
        Long chatRoomId = extractRoomId(session);
        
        if (memberId == null) {
            log.warn("WebSocket 연결 실패 - memberId 누락");
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        
        if (chatRoomId == null) {
            log.warn("WebSocket 연결 실패 - chatRoomId 누락 또는 잘못된 형식");
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        
        // 세션 등록
        roomSessions.computeIfAbsent(chatRoomId,
                        k -> Collections.synchronizedList(new ArrayList<>()))
                .add(session);
        log.info("채팅방 {} 에 사용자 {} 연결됨", chatRoomId, memberId);
        
        // SYSTEM 메시지 전송
        String nickname = memberRepository.findById(memberId).orElseThrow().getNickname();
        chatMessageService.sendSystemMessage(chatRoomId, nickname + "님이 입장했습니다.");
    }
    
    private Long extractRoomId(WebSocketSession session) {
        
        if (session.getUri() == null) {
            return null;
        }
        
        String query = session.getUri().getQuery();
        
        if (query == null) {
            return null;
        }
        
        for (String param : query.split("&")) {
            if (param.startsWith("roomId=")) {
                try {
                    return Long.valueOf(param.split("=")[1]);
                } catch (NumberFormatException e) {
                    log.warn("roomId 파싱 실패: {}", param);
                    return null;
                }
            }
        }
        return null;
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        try {
            
            String payload = textMessage.getPayload();
            log.debug("메시지 수신: {}", payload);
            
            // JSON -> DTO 변환
            ChatMessageRequest request = objectMapper.readValue(payload, ChatMessageRequest.class);
            
            if (request.getType() == null) {
                log.warn("메시지 타입 누락: {}", payload);
                return;
            }
            
            // 메시지 선언
            ChatMessage savedMessage;
            
            switch (request.getType()) {
                case TEXT:
                    savedMessage = chatMessageService.saveTextMessage(request);
                    break;
                case IMAGE:
                case FILE:
                    savedMessage = chatMessageService.saveFileMessage(request);
                    break;
                default:
                    log.warn("지원하지 않는 메시지 타입: {}", request.getType());
                    return;
            }
            
            // 메시지 변환
            ChatMessageResponse response = ChatMessageResponse.from(savedMessage);
            String responseJson = objectMapper.writeValueAsString(response);
            
            // 채팅방 참여자에게 브로드캐스트
            broadcastMessage(request.getChatRoomId(), responseJson);
        } catch (Exception e) {
            log.error("메시지 처리 중 에러 발생", e);
        }
    }
    
    private void broadcastMessage(Long chatRoomId, String messageJson) {
        List<WebSocketSession> sessions = roomSessions.get(chatRoomId);
        
        if (sessions == null) {
            return;
        }
        
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(messageJson));
                }
            } catch (Exception e) {
                log.warn("메시지 전송 실패: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        
        log.info("연결 종료: {}, status: {}", session.getId(), status);
        
        Long memberId = getMemberId(session);
        Long chatRoomId = extractRoomId(session);
        
        // 모든 채팅방 세션 목록에서 세션 제거
        roomSessions.forEach((roomId, sessions) -> {
            synchronized (sessions) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    roomSessions.remove(roomId);
                    log.info("세션이 비어있는 채팅방 세션 목록 제거. chatRoomId: {}", roomId);
                }
            }
        });
        
        // SYSTEM 메시지 전송
        String nickname = memberRepository.findById(memberId).orElseThrow().getNickname();
        chatMessageService.sendSystemMessage(chatRoomId, nickname + "님이 퇴장하셨습니다.");
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("에러 발생: {}", exception.getMessage());
    }
}
