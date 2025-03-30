package org.example.back.exception.base;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorcode;
    
    protected CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorcode = errorCode;
    }
}
