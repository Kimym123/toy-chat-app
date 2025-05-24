package org.example.back.repository.message;

import org.example.back.domain.message.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    int countByChatRoomIdAndIdGreaterThan(Long chatRoomId, Long messageId);
}
