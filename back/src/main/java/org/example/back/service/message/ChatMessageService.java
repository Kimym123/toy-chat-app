package org.example.back.service.message;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import org.example.back.domain.message.ChatMessage;
import org.example.back.dto.message.request.ChatMessageEditRequest;
import org.example.back.dto.message.request.ChatMessageRequest;
import org.example.back.dto.message.response.ChatMessageResponse;
import org.springframework.data.domain.Pageable;

public interface ChatMessageService {
    
    // 채팅 메시지를 저장한다.
    @Operation(summary = "채팅 메시지 저장", description = "채탕방 ID, 전송자 ID, 내용 등을 받아 메시지를 저장한다.")
    ChatMessage saveMessage(
            @Parameter(description = "메시지 저장 요청 DTO") ChatMessageRequest request
    );
    
    // saveMessage 재활용하여 텍스트 메시지를 저장한다.
    @Operation(summary = "텍스트 메시지 저장", description = "TEXT 메시지를 저장한다.")
    ChatMessage saveTextMessage(
            @Parameter(description = "메시지 요청 DTO") ChatMessageRequest request
    );
    
    // saveMessage 재활용하여 파일, 이미지 메시지를 저장한다.
    @Operation(summary = "파일/이미지 메시지 저장", description = "FILE, IMAGE 메시지를 저장한다.")
    ChatMessage saveFileMessage(
            @Parameter(description = "메시지 요청 DTO") ChatMessageRequest request
    );
    
    // 채팅방의 메시지를 페이징 형태로 조회한다.
    @Operation(summary = "채팅 메시지 목록 조회", description = "특정 채팅방의 메시지들을 페이지 단위로 조회한다.")
    List<ChatMessageResponse> getMessages(
            @Parameter(description = "채팅방 ID") Long chatRoomId,
            @Parameter(description = "페이징 정보") Pageable pageable
    );
    
    // 최근 채팅 메시지 N 개를 조회한다.
    @Operation(summary = "최근 채팅 메시지 조회", description = "채팅방에서 가장 최신 메시지 N 개를 조회한다.")
    List<ChatMessageResponse> getRecentMessages(
            @Parameter(description = "채팅방 ID") Long chatRoomId,
            @Parameter(description = "조회할 메시지 수") int limit
    );
    
    // 시스템 메시지를 전송한다.
    @Operation(summary = "시스템 메시지 전송 및 브로드캐스트", description = "입장/퇴장 등의 시스템 메시지를 저장하고, 채팅방의 모든 사용자에게 브로드캐스트한다.")
    void sendSystemMessageAndBroadcast(
            @Parameter(description = "채팅방 ID") Long chatRoomId,
            @Parameter(description = "메시지 내용") String content
    );
    
    // 채팅 메시지를 수정한다.
    @Operation(summary = "채팅 메시지 수정", description = "이미 작성한 채팅 메시지를 수정한다.")
    ChatMessageResponse editMessage(Long memberId, Long messageId, ChatMessageEditRequest request);
}
