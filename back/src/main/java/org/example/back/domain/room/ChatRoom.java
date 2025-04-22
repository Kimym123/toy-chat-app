package org.example.back.domain.room;

import jakarta.persistence.*;
import lombok.*;
import org.example.back.domain.base.BaseTimeEntity;

/**
 * ChatRoom 엔티티
 * - 실시간 채팅의 방 정보를 관리하는 도메인
 * - PRIVATE(1:1), GROUP(그룹) 타입
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "chat_room")
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 채팅방 타입: PRIVATE or GROUP
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;
    
    // 채팅방 이름 (GROUP 채팅에서만 사용됨)
    @Column(length = 255)
    private String name;
    
    // 삭제된 방인지 여부 (soft delete 구현용)
    @Column(nullable = false)
    private Boolean isDeleted = false;
    
    // 1:1 채팅방 생성 메서드 이름 없이 PRIVATE 타입으로 생성됨
    public static ChatRoom createPrivateRoom() {
        return ChatRoom.builder()
                .type(ChatRoomType.PRIVATE)
                .isDeleted(false)
                .build();
    }
    
    // 그룹 채팅방 생성 메서드 이름을 받아 GROUP 타입으로 생성됨
    public static ChatRoom createGroupRoom(String name) {
        return ChatRoom.builder()
                .type(ChatRoomType.GROUP)
                .name(name)
                .isDeleted(false)
                .build();
    }
    
    // soft delete 처리 메서드, 실제 삭제하지 않고 isDeleted 플래그만 true 로 변경
    public void markAsDeleted() {
        this.isDeleted = true;
    }
}
