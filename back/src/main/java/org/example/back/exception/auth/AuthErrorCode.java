package org.example.back.exception.auth;

import lombok.Getter;
import org.example.back.exception.base.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode implements ErrorCode {
    
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 RefreshToken 입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "저장된 RefreshToken 이 존재하지 않습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "RefreshToken 이 일치하지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자가 존재하지 않습니다.");
    
    private final HttpStatus status;
    private final String message;
    
    AuthErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
