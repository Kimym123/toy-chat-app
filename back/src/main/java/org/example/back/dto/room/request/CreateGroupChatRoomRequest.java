package org.example.back.dto.room.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "그룹 채팅방 생성 요청 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGroupChatRoomRequest {
    
    @NotBlank
    @Schema(description = "채팅방 이름", example = "스터디 그룹")
    private String name;
    
    @NotEmpty
    @Schema(description = "참여할 회원 ID 목록", example = "[1,2,3]")
    private List<Long> memberIds;
}
