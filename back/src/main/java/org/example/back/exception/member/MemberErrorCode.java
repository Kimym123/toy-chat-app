package org.example.back.exception.member;

import lombok.Getter;
import org.example.back.exception.base.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum MemberErrorCode implements ErrorCode {
    
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 존재하는 사용자 이름입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 존재하는 닉네임입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    DUPLICATE_PHONE(HttpStatus.CONFLICT, "이미 존재하는 전화번호입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
    
    private final HttpStatus status;
    private final String message;
    
    MemberErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
