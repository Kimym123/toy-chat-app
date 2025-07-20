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
public class ChatMessageDeleteEvent {
    
    private Long messageId;
    private Long chatRoomId;
    private Long deletedBy;
    private String deletedByNickname;
    private LocalDateTime deletedAt;
    private String eventType = "MESSAGE_DELETED";
    
    public static ChatMessageDeleteEvent of(Long messageId, Long chatRoomId, Long deletedBy,
            String deletedByNickname) {
        return ChatMessageDeleteEvent.builder()
                .messageId(messageId)
                .chatRoomId(chatRoomId)
                .deletedBy(deletedBy)
                .deletedByNickname(deletedByNickname)
                .deletedAt(LocalDateTime.now())
                .eventType("MESSAGE_DELETED")
                .build();
    }
}