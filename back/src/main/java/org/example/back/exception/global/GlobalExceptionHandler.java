package org.example.back.exception.global;

import org.example.back.exception.ErrorResponse;
import org.example.back.exception.base.CustomException;
import org.example.back.exception.base.ErrorCode;
import org.example.back.exception.member.MemberException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ErrorResponse> handleMemberException(MemberException exception) {
        ErrorCode errorCode = exception.getErrorcode();
        // 현재는 handleCustomException 와 다를 것이 없지만 어떻게 확장할지 몰라서 일단은 만들어놓음.
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode.getStatus().value(), errorCode.getMessage()));
    }
    
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorcode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode.getStatus().value(), errorCode.getMessage()));
    }
}
