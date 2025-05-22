package org.example.back.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.repository.MemberRepository;
import org.example.back.service.message.ChatMessageService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompConnectListener {
    
    private final ChatMessageService chatMessageService;
    private final MemberRepository memberRepository;
    
    @EventListener
    public void handleStompConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        
        // 입장 시 등록한 session attribute
        String memberIdStr = accessor.getFirstNativeHeader("memberId");
        String chatRoomIdStr = accessor.getFirstNativeHeader("chatRoomId");
        
        if (memberIdStr == null && chatRoomIdStr == null) {
            log.warn("입장 실패 - 필수 헤더 누락 (memberId: {}, roomId: {})", memberIdStr, chatRoomIdStr);
            return;
        }
        
        Long memberId = Long.valueOf(memberIdStr);
        Long chatRoomId = Long.valueOf(chatRoomIdStr);
        
        String nickname = memberRepository.findById(memberId).orElseThrow().getNickname();
        chatMessageService.sendSystemMessageAndBroadcast(chatRoomId, nickname + "님이 입장했습니다.");
        
        log.info("입장: memberId={}, roomId={}, nickname={}", memberId, chatRoomId, nickname);
    }
}
