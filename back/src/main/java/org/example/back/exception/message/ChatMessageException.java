package org.example.back.exception.message;

import org.example.back.exception.base.CustomException;

public class ChatMessageException extends CustomException {

    public ChatMessageException(ChatMessageErrorCode errorCode) {
        super(errorCode);
    }

    public ChatMessageException(ChatMessageErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
