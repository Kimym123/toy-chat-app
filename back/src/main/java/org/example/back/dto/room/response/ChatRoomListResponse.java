package org.example.back.dto.room.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "채팅방 목록 조회 응답 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomListResponse {
    
    @Schema(description = "채팅방 응답 목록")
    private List<ChatRoomResponse> chatRooms;
    
    @Schema(description = "총 채팅방 수", example = "5")
    private long totalCount;
}
