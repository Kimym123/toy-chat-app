package org.example.back.domain.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.back.domain.base.BaseTimeEntity;
import org.example.back.domain.member.Member;

/**
 * ChatParticipant 엔티티
 * - Member ↔ ChatRoom 다대다(N:N) 관계를 위한 중간 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "chat_participant")
public class ChatParticipant extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 참여자 정보, 다대일 관계 (한 유저는 여러 채팅방 참여 가능), 지연 로딩 (성능 최적화)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    // 채팅방 정보, 다대일 관계 (한 채팅방에 여러 사용자 참여 가능), 지연 로딩
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;
    
    // 읽음 처리용
    @Column
    private Long lastReadMessageId;
    
    // 메시지 읽음 상태 업데이트, 사용자가 마지막으로 읽은 메시지 ID 갱
    public void updateLastReadMessage(Long messageId) {
        this.lastReadMessageId = messageId;
    }
    
    public static ChatParticipant create(ChatRoom chatRoom, Member member) {
        return ChatParticipant.builder()
                .chatRoom(chatRoom)
                .member(member)
                .build();
    }
}
