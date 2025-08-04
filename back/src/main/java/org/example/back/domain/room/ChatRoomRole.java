package org.example.back.domain.room;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 채팅방 레벨 사용자 역할
 * - 특정 채팅방 내에서 사용자가 가지는 권한을 정의
 * - 각 채팅방마다 독립적으로 부여되는 권한
 */
@Schema(description = "채팅방 레벨 사용자 역할")
@Getter
@RequiredArgsConstructor
public enum ChatRoomRole {
    
    @Schema(description = "일반 멤버 - 채팅방에서 메시지 전송 및 기본 기능만 사용 가능")
    MEMBER("일반 멤버", 1),
    
    @Schema(description = "부방장 - 해당 채팅방에서 메시지 삭제, 사용자 강퇴 등 부방장 권한 보유")
    MODERATOR("부방장", 2),
    
    @Schema(description = "방장 - 해당 채팅방의 모든 권한 보유 (설정 변경, 부방장 임명, 방 삭제 등)")
    OWNER("방장", 3);
    
    private final String displayName;
    private final int level;
    
    /**
     * 다른 역할에 대해 관리 권한이 있는지 확인
     *
     * @param targetRole 대상 역할
     * @return 관리 권한 여부
     */
    public boolean canManage(ChatRoomRole targetRole) {
        return this.level > targetRole.level;
    }
    
    /**
     * 최소 요구 레벨을 만족하는지 확인
     *
     * @param requiredLevel 최소 요구 레벨
     * @return 레벨 만족 여부
     */
    public boolean hasLevel(int requiredLevel) {
        return this.level >= requiredLevel;
    }
    
    /**
     * 부방장 권한이 있는지 확인 (MODERATOR 이상)
     *
     * @return 부방장 권한 여부
     */
    public boolean canModerate() {
        return this.level >= MODERATOR.level;
    }
    
    /**
     * 방 관리 권한이 있는지 확인 (OWNER만 가능)
     *
     * @return 방 관리 권한 여부
     */
    public boolean canManageRoom() {
        return this == OWNER;
    }
}