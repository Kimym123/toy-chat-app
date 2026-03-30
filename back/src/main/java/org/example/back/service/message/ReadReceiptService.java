package org.example.back.service.message;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.example.back.domain.message.TypingStatus;
import org.example.back.dto.websocket.request.ReadMessageRequest;
import org.example.back.dto.websocket.response.ReadReceiptResponse;

import java.util.List;

public interface ReadReceiptService {
    
    // 메시지 읽음 처리 수행
    @Operation(summary = "읽음 상태 업데이트", description = "메시지 유효성 확인 후 읽음 상태를 업데이트하고 브로드캐스트한다.")
    void updateLastReadMessageId(
            @Parameter(description = "읽음 처리 요청 DTO") ReadMessageRequest request
    );
    
    // 채팅방 참여자들의 읽음 상태 목록 조회
    @Operation(summary = "채팅방 읽음 상태 조회", description = "채팅방 참여자들의 마지막 읽은 메시지 정보를 반환한다.")
    List<ReadReceiptResponse> getReadStatuses(
            @Parameter(description = "채팅방 ID") Long chatRoomId,
            @Parameter(description = "요청자 ID") Long memberId
    );
    
    // 사용자의 채팅방 내 안읽은 메시지 수 반환
    @Operation(summary = "안읽은 메시지 수 조회", description = "해당 채팅방에서 사용자가 읽지 않은 메시지 수를 반환한다.")
    int getUnreadMessageCount(
            @Parameter(description = "채팅방 ID") Long chatRoomId,
            @Parameter(description = "회원 ID") Long memberId
    );
    
    // 실시간 입력 상태 타이핑 브로드캐스트
    @Operation(summary = "타이핑 상태 브로드캐스트", description = "채팅방 참여자들에게 타이핑 상태를 실시간 전송한다.")
    void broadcastTypingStatus(
            @Parameter(description = "채팅방 ID") Long chatRoomId,
            @Parameter(description = "회원 ID") Long memberId,
            @Parameter(description = "타이핑 상태") TypingStatus status
    );
}
