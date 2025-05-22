package org.example.back.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.repository.MemberRepository;
import org.example.back.service.message.ChatMessageService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompDisconnectListener {
    
    private final ChatMessageService chatMessageService;
    private final MemberRepository memberRepository;
    
    @EventListener
    public void handleStompDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        
        // 입장 시 등록한 session attribute
        Long memberId = (Long) accessor.getSessionAttributes().get("memberId");
        Long chatRoomId = (Long) accessor.getSessionAttributes().get("chatRoomId");
        
        if (memberId == null || chatRoomId == null) {
            log.warn("퇴장 실패 - session attribute 누락");
            return;
        }
        
        String nickname = memberRepository.findById(memberId).orElseThrow().getNickname();
        chatMessageService.sendSystemMessageAndBroadcast(chatRoomId, nickname + "님이 퇴장하셨습니다.");
        
        log.info("퇴장: memberId={}, roomId={}, nickname={}", memberId, chatRoomId, nickname);
    }
}
