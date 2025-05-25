package org.example.back.dto.message.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.back.domain.message.TypingStatus;

@Schema(description = "사용자 타이핑 요청 DTO")
@Getter
@NoArgsConstructor
public class TypingStatusRequest {
    @Schema(description = "채팅방 ID", example = "101")
    private Long chatRoomId;
    
    @Schema(description = "입력 상태", example = "typing", allowableValues = {"typing", "stop"})
    private String status;
    
    public TypingStatus getTypingStatusEnum() {
        return TypingStatus.from(status);
    }
}
