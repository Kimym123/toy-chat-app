package org.example.back.domain.room;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Schema(description = "채팅방 참여자 정보 - Member와 ChatRoom의 다대다(N:M) 관계를 위한 중간 엔티티")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "chat_participant")
public class ChatParticipant extends BaseTimeEntity {
    
    @Schema(description = "채팅방 참여 기록 ID (Member와 ChatRoom의 관계를 식별하는 고유 키)", example = "1")
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 참여자 정보, 다대일 관계 (한 유저는 여러 채팅방 참여 가능), 지연 로딩 (성능 최적화)
    @Schema(description = "회원 ID", example = "1")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    // 채팅방 정보, 다대일 관계 (한 채팅방에 여러 사용자 참여 가능), 지연 로딩
    @Schema(description = "채팅방 ID", example = "101")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;
    
    // 읽음 처리용
    @Schema(description = "가장 마지막으로 읽은 메시지 ID", example = "1000")
    @Column
    private Long lastReadMessageId;
    
    // 메시지 읽음 상태 업데이트, 사용자가 마지막으로 읽은 메시지 ID 갱신
    public void updateLastReadMessage(Long messageId) {
        this.lastReadMessageId = messageId;
    }
    
    // 채팅방에서의 역할
    @Schema(description = "채팅방에서의 역할", example = "MEMBER")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ChatRoomRole role = ChatRoomRole.MEMBER;
    
    
    /**
     * 채팅방 역할 변경
     *
     * @param newRole 새로운 역할
     */
    public void updateRole(ChatRoomRole newRole) {
        this.role = newRole;
    }
    
    /**
     * 부방장 권한이 있는지 확인
     *
     * @return 부방장 권한 여부 (MODERATOR 이상)
     */
    public boolean canModerate() {
        return this.role.canModerate();
    }
    
    /**
     * 방 관리 권한이 있는지 확인
     *
     * @return 방 관리 권한 여부 (OWNER만 가능)
     */
    public boolean canManageRoom() {
        return this.role.canManageRoom();
    }
    
    /**
     * 다른 참여자를 관리할 수 있는지 확인
     *
     * @param targetParticipant 대상 참여자
     * @return 관리 권한 여부
     */
    public boolean canManage(ChatParticipant targetParticipant) {
        return this.role.canManage(targetParticipant.role);
    }
    
    public static ChatParticipant create(ChatRoom chatRoom, Member member) {
        return ChatParticipant.builder()
                .chatRoom(chatRoom)
                .member(member)
                .role(ChatRoomRole.MEMBER) // 기본값 명시
                .build();
    }
    
    /**
     * 채팅방 소유자로 생성
     *
     * @param chatRoom 채팅방
     * @param member 회원
     * @return 소유자 권한을 가진 참여자
     */
    public static ChatParticipant createOwner(ChatRoom chatRoom, Member member) {
        return ChatParticipant.builder()
                .chatRoom(chatRoom)
                .member(member)
                .role(ChatRoomRole.OWNER)
                .build();
    }
    
    /**
     * 채팅방 모더레이터로 생성
     *
     * @param chatRoom 채팅방
     * @param member 회원
     * @return 모더레이터 권한을 가진 참여자
     */
    public static ChatParticipant createModerator(ChatRoom chatRoom, Member member) {
        return ChatParticipant.builder()
                .chatRoom(chatRoom)
                .member(member)
                .role(ChatRoomRole.MODERATOR)
                .build();
    }
}
