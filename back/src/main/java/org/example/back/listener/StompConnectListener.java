package org.example.back.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.exception.member.MemberErrorCode;
import org.example.back.exception.member.MemberException;
import org.example.back.repository.MemberRepository;
import org.example.back.service.message.ChatMessageService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompConnectListener {

    private final ChatMessageService chatMessageService;
    private final MemberRepository memberRepository;

    @EventListener
    public void handleStompConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.warn("입장 실패 - 세션 없음");
            return;
        }

        Object memberIdObj = sessionAttributes.get("memberId");
        if (!(memberIdObj instanceof Long memberId)) {
            log.warn("입장 실패 - 세션에 memberId 없음 또는 타입 불일치");
            return;
        }

        String chatRoomIdStr = accessor.getFirstNativeHeader("chatRoomId");
        if (chatRoomIdStr == null) {
            log.warn("입장 실패 - chatRoomId 헤더 누락 (memberId: {})", memberId);
            return;
        }

        Long chatRoomId;
        try {
            chatRoomId = Long.valueOf(chatRoomIdStr);
        } catch (NumberFormatException e) {
            log.warn("입장 실패 - 잘못된 chatRoomId 형식 (memberId: {}, chatRoomId: {})", memberId, chatRoomIdStr);
            return;
        }

        // 퇴장 시 사용할 수 있도록 세션에 chatRoomId 저장
        sessionAttributes.put("chatRoomId", chatRoomId);

        String nickname = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.USER_NOT_FOUND))
                .getNickname();
        chatMessageService.sendSystemMessageAndBroadcast(chatRoomId, nickname + "님이 입장했습니다.");

        log.info("입장: memberId={}, roomId={}, nickname={}", memberId, chatRoomId, nickname);
    }
}
