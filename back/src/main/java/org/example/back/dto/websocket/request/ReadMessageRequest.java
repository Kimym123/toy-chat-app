package org.example.back.dto.websocket.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "채팅방 메시지 읽음 처리 요청 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadMessageRequest {
    
    @Schema(description = "읽음 처리할 채팅방 ID", example = "101")
    private Long chatRoomId;
    
    @Schema(description = "읽음 처리를 요청하는 회원 ID", example = "1")
    private Long memberId;
    
    @Schema(description = "읽음 처리할 메시지 ID", example = "1001")
    private Long messageId;
}
