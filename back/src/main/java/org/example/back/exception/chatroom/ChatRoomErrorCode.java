package org.example.back.exception.chatroom;

import lombok.Getter;
import org.example.back.exception.base.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum ChatRoomErrorCode implements ErrorCode {

    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    ROOM_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "채팅방 이름은 비어 있을 수 없습니다."),
    INVALID_MEMBER_IDS(HttpStatus.BAD_REQUEST, "일부 회원 ID가 유효하지 않습니다."),
    PRIVATE_ROOM_INVITE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "1:1 채팅방에는 초대할 수 없습니다."),
    ALREADY_DELETED_ROOM(HttpStatus.BAD_REQUEST, "이미 삭제된 채팅방입니다."),
    DELETED_ROOM(HttpStatus.BAD_REQUEST, "삭제된 채팅방에는 메시지를 보낼 수 없습니다."),
    NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "해당 사용자는 채팅방의 참여자가 아닙니다."),
    NO_PARTICIPANTS(HttpStatus.NOT_FOUND, "채팅방에 참여자가 없습니다."),
    SELF_CHAT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신과는 채팅방을 만들 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ChatRoomErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
