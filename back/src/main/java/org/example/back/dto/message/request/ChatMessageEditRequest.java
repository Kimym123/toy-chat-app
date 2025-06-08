package org.example.back.dto.message.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "채팅 메시지 수정 요청 DTO")
@Getter
@NoArgsConstructor
public class ChatMessageEditRequest {
    
    @Schema(description = "수정할 메시지의 고유 ID", example = "1024")
    @NotNull(message = "메시지 ID는 필수입니다.")
    private Long messageId;
    
    @Schema(description = "새로운 메시지 내용", example = "수정된 메시지입니다.")
    @NotBlank(message = "메시지 내용은 비어 있을 수 없습니다.")
    private String newContent;
}
