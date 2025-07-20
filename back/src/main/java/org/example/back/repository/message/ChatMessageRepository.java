package org.example.back.repository.message;

import java.util.Optional;
import org.example.back.domain.message.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // 안 읽은 메시지 수 계산
    int countByChatRoomIdAndIdGreaterThan(Long chatRoomId, Long messageId);
    
    // clientMessageId 중복 여부 확인용
    boolean existsByClientMessageId(String clientMessageId);
    
    // 메시지 중복 저장 방지
    Optional<ChatMessage> findByClientMessageId(String clientMessageId);
}
