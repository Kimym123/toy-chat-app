package org.example.back.dto.message.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.back.domain.file.UploadedFile;
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

    @Schema(description = "첨부 파일 정보 (FILE / IMAGE 메시지에만 존재)")
    private FileDto file;

    @Getter
    @Builder
    @Schema(description = "첨부 파일 DTO")
    public static class FileDto {

        @Schema(description = "파일 ID", example = "42")
        private Long fileId;

        @Schema(description = "다운로드 URL (인증 필요)", example = "/api/files/42")
        private String downloadUrl;

        @Schema(description = "원본 파일명", example = "내사진.jpg")
        private String originalName;

        @Schema(description = "파일 크기 (바이트)", example = "204800")
        private Long size;
    }

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
        boolean deleted = message.isDeleted();

        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .content(deleted ? "삭제된 메시지입니다." : message.getContent())
                .type(deleted ? MessageType.SYSTEM : message.getMessageType())
                .createdAt(message.getCreatedAt())
                .sender(buildSenderDto(sender))
                .clientMessageId(message.getClientMessageId())
                .file(deleted ? null : buildFileDto(message.getFile()))
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

    private static FileDto buildFileDto(UploadedFile file) {
        if (file == null) {
            return null;
        }
        return FileDto.builder()
                .fileId(file.getId())
                .downloadUrl("/api/files/" + file.getId())
                .originalName(file.getOriginalName())
                .size(file.getSize())
                .build();
    }
}
