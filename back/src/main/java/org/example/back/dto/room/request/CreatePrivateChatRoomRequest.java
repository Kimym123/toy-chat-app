package org.example.back.dto.room.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "1:1 채팅방 생성 요청 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePrivateChatRoomRequest {
    
    @NotNull
    @Schema(description = "내 Member ID", example = "1")
    private Long memberId;
    
    @NotNull
    @Schema(description = "상대방 Member ID", example = "2")
    private Long targetMemberId;
}
