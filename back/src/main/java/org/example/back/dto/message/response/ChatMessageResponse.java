package org.example.back.dto.message.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.back.domain.member.Member;
import org.example.back.domain.message.ChatMessage;
import org.example.back.domain.message.MessageType;

@Schema(description = "채팅 메시지 응답 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    
    @Schema(description = "메시지 ID", example = "101")
    private Long messageId;
    
    @Schema(description = "채팅방 ID", example = "1")
    private Long chatRoomId;
    
    @Schema(description = "메시지 내용", example = "테스트입니다!")
    private String content;
    
    @Schema(description = "메시지 타입", example = "TEXT / IMAGE / FILE / SYSTEM")
    private MessageType type;
    
    @Schema(description = "메시지 생성 시각")
    private LocalDateTime createdAt;
    
    @Schema(description = "메시지 전송자 정보")
    private SenderDto sender;
    
    @Schema(description = "클라이언트 메시지 식별자", example = "550e8400-e29b-41d4-a716-446655440000")
    private String clientMessageId;
    
    @Getter
    @Builder
    @Schema(description = "메시지 전송자 DTO")
    public static class SenderDto {
        
        @Schema(description = "보낸 사람 ID", example = "1")
        private Long id;
        
        @Schema(description = "보낸 사람 닉네임", example = "Test")
        private String username;
        
        @Schema(description = "프로필 이미지 URL", example = "http://exmple.com/image.jpg")
        private String profileImageUrl;
    }
    
    public static ChatMessageResponse from(ChatMessage message) {
        Member sender = message.getSender();
        
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .content(message.isDeleted() ? "삭제된 메시지입니다." : message.getContent())
                .type(message.isDeleted() ? MessageType.SYSTEM :
                        message.getMessageType())
                .createdAt(message.getCreatedAt())
                .sender(buildSenderDto(sender))
                .clientMessageId(message.getClientMessageId())
                .build();
    }
    
    private static SenderDto buildSenderDto(Member sender) {
        if (sender == null) {
            return null;
        }
        
        return SenderDto.builder()
                .id(sender.getId())
                .username(sender.getUsername())
                .profileImageUrl(sender.getProfileImageUrl())
                .build();
    }
}
