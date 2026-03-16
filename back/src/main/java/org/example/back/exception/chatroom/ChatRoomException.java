package org.example.back.exception.chatroom;

import org.example.back.exception.base.CustomException;

public class ChatRoomException extends CustomException {

    public ChatRoomException(ChatRoomErrorCode errorCode) {
        super(errorCode);
    }

    public ChatRoomException(ChatRoomErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
