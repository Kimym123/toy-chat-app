package org.example.back.exception.message;

import lombok.Getter;
import org.example.back.exception.base.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum ChatMessageErrorCode implements ErrorCode {

    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."),
    CLIENT_MESSAGE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "clientMessageId는 필수입니다."),
    CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "텍스트 메시지에는 content가 필요합니다."),
    FILE_URL_REQUIRED(HttpStatus.BAD_REQUEST, "파일 메시지에는 fileUrl이 필요합니다."),
    UNSUPPORTED_MESSAGE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 메시지 타입입니다."),
    NOT_DELETED_MESSAGE(HttpStatus.BAD_REQUEST, "삭제되지 않은 메시지입니다."),
    MESSAGE_NOT_IN_ROOM(HttpStatus.BAD_REQUEST, "해당 메시지가 요청한 채팅방에 속하지 않습니다."),
    MEMBER_ID_MISMATCH(HttpStatus.FORBIDDEN, "memberId가 일치하지 않습니다."),
    NOT_MESSAGE_OWNER(HttpStatus.FORBIDDEN, "본인의 메시지만 수정/삭제/복구할 수 있습니다."),
    SYSTEM_MESSAGE_NOT_DELETABLE(HttpStatus.BAD_REQUEST, "시스템 메시지는 삭제할 수 없습니다."),
    SYSTEM_MESSAGE_NOT_RESTORABLE(HttpStatus.BAD_REQUEST, "시스템 메시지는 복구할 수 없습니다."),
    EDIT_TIME_EXPIRED(HttpStatus.BAD_REQUEST, "메시지는 5분 이내에만 수정 가능합니다."),
    DELETE_TIME_EXPIRED(HttpStatus.BAD_REQUEST, "메시지는 5분 이내에만 삭제 가능합니다."),
    RESTORE_TIME_EXPIRED(HttpStatus.BAD_REQUEST, "메시지는 5분 이내에만 복구 가능합니다."),
    INVALID_TYPING_STATUS(HttpStatus.BAD_REQUEST, "올바르지 않은 타이핑 상태입니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다. 다시 연결해주세요.");

    private final HttpStatus status;
    private final String message;

    ChatMessageErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
