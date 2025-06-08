package org.example.back.domain.message;

import jakarta.persistence.*;
import lombok.*;
import org.example.back.domain.base.BaseTimeEntity;
import org.example.back.domain.member.Member;
import org.example.back.domain.room.ChatRoom;

/*
* ChatMessage 엔티티
* - 채팅방 내 메시지를 저장하는 도메인
* */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "chat_message")
public class ChatMessage extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    // 소속 채팅방
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;
    
    // 보낸 사람
    // SYSTEM 메시지 대응 위해 sender는 null 허용됨
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender;
    
    // 메시지 내용
    @Lob
    @Column(nullable = false)
    private String content;
    
    // 메시지 타입 (텍스트, 이미지, 파일 등등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;
    
    // 클라이언트 측 메시지 식별자 (중복 전송 방지용)
    @Column(name = "client_message_id", unique = true, nullable = false, length = 64)
    private String clientMessageId;
    
    // 낙관적 락 버전 필드 추가 (동시 수정 충돌 대비)
    @Version
    private Long version;
    
    // 메시지 내용 수정 메서드 추가
    public void updateContent(String newContent) {
        this.content = newContent;
    }
}
