package org.example.back.dto.room.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "채팅방 나가기 요청 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveChatRoomRequest {
    
    @NotNull
    @Schema(description = "나가는 회원 ID", example = "1")
    private Long memberId;
}
