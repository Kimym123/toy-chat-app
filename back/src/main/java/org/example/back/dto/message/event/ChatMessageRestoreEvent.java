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
public class ChatMessageRestoreEvent {
    
    private Long messageId;
    private Long chatRoomId;
    private Long restoredBy;
    private String restoredByNickname;
    private LocalDateTime restoredAt;
    private String eventType = "MESSAGE_RESTORED";
    
    public static ChatMessageRestoreEvent of(Long messageId, Long chatRoomId, Long restoredBy,
            String restoredByNickname) {
        return ChatMessageRestoreEvent.builder()
                .messageId(messageId)
                .chatRoomId(chatRoomId)
                .restoredBy(restoredBy)
                .restoredByNickname(restoredByNickname)
                .restoredAt(LocalDateTime.now())
                .eventType("MESSAGE_RESTORED")
                .build();
    }
}