package org.example.back.dto.message.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEvent {

    private Long messageId;
    private Long chatRoomId;
    private Long memberId;
    private String nickname;
    private String content;
    private LocalDateTime timestamp;
    private ChatMessageEventType eventType;

    private static ChatMessageEvent of(Long messageId, Long chatRoomId, Long memberId,
            String nickname, ChatMessageEventType eventType) {
        return ChatMessageEvent.builder()
                .messageId(messageId)
                .chatRoomId(chatRoomId)
                .memberId(memberId)
                .nickname(nickname)
                .timestamp(LocalDateTime.now())
                .eventType(eventType)
                .build();
    }

    public static ChatMessageEvent deleted(Long messageId, Long chatRoomId, Long memberId,
            String nickname) {
        return of(messageId, chatRoomId, memberId, nickname, ChatMessageEventType.MESSAGE_DELETED);
    }

    public static ChatMessageEvent edited(Long messageId, Long chatRoomId, Long memberId,
            String nickname, String content) {
        return ChatMessageEvent.builder()
                .messageId(messageId)
                .chatRoomId(chatRoomId)
                .memberId(memberId)
                .nickname(nickname)
                .content(content)
                .timestamp(LocalDateTime.now())
                .eventType(ChatMessageEventType.MESSAGE_EDITED)
                .build();
    }

    public static ChatMessageEvent restored(Long messageId, Long chatRoomId, Long memberId,
            String nickname) {
        return of(messageId, chatRoomId, memberId, nickname, ChatMessageEventType.MESSAGE_RESTORED);
    }
}
