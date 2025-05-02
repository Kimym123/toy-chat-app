package org.example.back.dto.room.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.back.domain.room.ChatRoom;


@Schema(description = "채팅방 단일 조회 응답 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponse {
    
    @Schema(description = "채팅방 ID", example = "1")
    private Long roomId;
    
    @Schema(description = "채팅방 타입 (PRIVATE, GROUP)", example = "PRIVATE")
    private String type;
    
    @Schema(description = "채팅방 이름 (1:1 채팅은 null)", example = "스터디 그룹")
    private String name;
    
    @Schema(description = "참여자 ID 목록", example = "[1, 2]")
    private List<Long> participantIds;
    
    public static ChatRoomResponse from(ChatRoom chatRoom, List<Long> participantIds) {
        return ChatRoomResponse.builder()
                .roomId(chatRoom.getId())
                .type(chatRoom.getType().name())
                .name(chatRoom.getName())
                .participantIds(participantIds)
                .build();
    }
}
