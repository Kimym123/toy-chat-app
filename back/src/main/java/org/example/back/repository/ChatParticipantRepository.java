package org.example.back.repository;

import java.util.List;
import java.util.Optional;
import org.example.back.domain.room.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    
    // 특정 채팅방에 속한 모든 참여자 조회
    List<ChatParticipant> findByChatRoomId(Long chatRoomId);
    
    // 특정 채팅방 + 특정 사용자 조합으로 참여자 찾기
    Optional<ChatParticipant> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);
    
    // 특정 사용자가 속한 모든 채팅방 ID 조회
    List<ChatParticipant> findByMemberId(Long memberId);
    
    // 특정 사용자 + 채팅방 조합으로 참여자 삭제 (나가기 기능)
    void deleteByMemberIdAndChatRoomId(Long memberId, Long chatRoomId);
    
    // 특정 채팅방에 남은 참여자 수
    long countByChatRoomId(Long chatRoomId);
}
