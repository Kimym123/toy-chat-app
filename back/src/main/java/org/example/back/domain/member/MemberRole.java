package org.example.back.domain.member;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시스템 레벨 사용자 역할
 * - 전체 시스템에서 사용자가 가지는 기본 권한을 정의
 * - 레벨이 높을수록 더 많은 권한을 가짐
 */
@Schema(description = "시스템 레벨 사용자 역할")
@Getter
@RequiredArgsConstructor
public enum MemberRole {
    
    @Schema(description = "일반 사용자 - 기본 채팅 기능만 사용 가능")
    USER("일반 사용자", 1),
    
    @Schema(description = "모더레이터 - 모든 채팅방에서 모더레이션 권한 보유")
    MODERATOR("모더레이터", 2),
    
    @Schema(description = "관리자 - 시스템 전체 관리 권한 보유")
    ADMIN("관리자", 3);
    
    private final String displayName;
    private final int level;
    
    /**
     * 다른 역할에 대해 관리 권한이 있는지 확인
     *
     * @param targetRole 대상 역할
     * @return 관리 권한 여부
     */
    public boolean canManage(MemberRole targetRole) {
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
     * 관리자급 권한인지 확인 (MODERATOR 이상)
     *
     * @return 관리자급 권한 여부
     */
    public boolean isStaff() {
        return this.level >= MODERATOR.level;
    }
}