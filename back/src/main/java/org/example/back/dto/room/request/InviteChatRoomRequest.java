package org.example.back.dto.room.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "채팅방 초대 요청 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteChatRoomRequest {
    
    @NotEmpty
    @Schema(description = "초대할 회원 ID 목록", example = "[4,5]")
    private List<Long> memberIds;
}
