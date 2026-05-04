package org.example.back.dto.message.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.back.domain.message.MessageType;
import org.example.back.validation.ValidChatMessage;

@Schema(description = "채팅 메시지 요청 DTO")
@ValidChatMessage
@Getter
@NoArgsConstructor
public class ChatMessageRequest {
    
    @Schema(description = "채팅방 ID", example = "100")
    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;
    
    @Setter
    @Schema(description = "보내는 회원 ID", example = "1")
    private Long senderId;
    
    @Schema(description = "메시지 내용", example = "테스트입니다.")
    @Setter
    @Size(max = 1000, message = "메시지는 1000자를 초과할 수 없습니다.")
    private String content;
    
    @Schema(description = "메시지 타입 (TEXT, IMAGE, FILE)", example = "TEXT")
    @NotNull(message = "메시지 타입은 필수입니다.")
    private MessageType type;
    
    @Schema(description = "첨부 파일 ID (파일/이미지 메시지일 경우 필수, 업로드 API 응답의 fileId)", example = "42")
    private Long fileId;
    
    @Schema(description = "클라이언트 메시지 식별자 (중복 전송 방지용)", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "클라이언트 메시지 식별자는 필수입니다.")
    private String clientMessageId;
}
