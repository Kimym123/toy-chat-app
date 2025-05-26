package org.example.back.dto.message.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.back.domain.message.MessageType;

@Schema(description = "채팅 메시지 요청 DTO")
@Getter
@NoArgsConstructor
public class ChatMessageRequest {
    
    @Schema(description = "채팅방 ID", example = "100")
    private Long chatRoomId;
    
    @Setter
    @Schema(description = "보내는 회원 ID", example = "1")
    private Long senderId;
    
    @Schema(description = "메시지 내용", example = "테스트입니다.")
    @Setter
    private String content;
    
    @Schema(description = "메시지 타입 (TEXT, IMAGE, FILE)", example = "TEXT")
    private MessageType type;
    
    @Schema(description = "파일 URL (파일/이미지 메시지일 경우 사용)", example = "http://localhost:8080/files/abc123.jpg")
    private String fileUrl;
    
    @Schema(description = "클라이언트 메시지 식별자 (중복 전송 방지용)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String clientMessageId;
}
