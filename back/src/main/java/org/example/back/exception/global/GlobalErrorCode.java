package org.example.back.exception.global;

import lombok.Getter;
import org.example.back.exception.base.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum GlobalErrorCode implements ErrorCode {
    
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.");
    
    private final HttpStatus status;
    private final String message;
    
    GlobalErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
