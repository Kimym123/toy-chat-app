package org.example.back.dto.websocket.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "채팅방 메시지 읽음 처리 응답 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadReceiptResponse {
    
    @Schema(description = "메시지가 속한 채팅방 ID", example = "101")
    private Long chatRoomId;
    
    @Schema(description = "메시지를 읽은 회원 ID", example = "1")
    private Long memberId;
    
    @Schema(description = "읽은 마지막 메시지 ID", example = "1001")
    private Long messageId;
}
