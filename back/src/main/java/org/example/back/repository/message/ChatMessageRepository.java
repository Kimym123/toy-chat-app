package org.example.back.repository.message;

import java.util.Optional;
import org.example.back.domain.message.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 안 읽은 메시지 수 계산
    int countByChatRoomIdAndIdGreaterThan(Long chatRoomId, Long messageId);

    // clientMessageId 중복 여부 확인용
    boolean existsByClientMessageId(String clientMessageId);

    // 메시지 중복 저장 방지
    Optional<ChatMessage> findByClientMessageId(String clientMessageId);

    // 다운로드 권한 검증: 해당 파일이 사용된 채팅방 중 호출자가 참여 중인 방이 있는가
    @Query("SELECT COUNT(p) > 0 " +
            "FROM ChatMessage m " +
            "JOIN ChatParticipant p ON p.chatRoom = m.chatRoom " +
            "WHERE m.file.id = :fileId AND p.member.id = :memberId")
    boolean existsParticipantAccessByFileIdAndMemberId(
            @Param("fileId") Long fileId,
            @Param("memberId") Long memberId
    );
}
