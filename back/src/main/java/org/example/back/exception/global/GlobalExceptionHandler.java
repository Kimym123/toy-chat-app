package org.example.back.exception.global;

import jakarta.persistence.OptimisticLockException;
import org.example.back.exception.ErrorResponse;
import org.example.back.exception.base.CustomException;
import org.example.back.exception.base.ErrorCode;
import org.example.back.exception.member.MemberException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ErrorResponse> handleMemberException(MemberException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        // 현재는 handleCustomException 와 다를 것이 없지만 어떻게 확장할지 몰라서 일단은 만들어놓음.
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode.getStatus().value(), errorCode.getMessage()));
    }
    
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode.getStatus().value(), errorCode.getMessage()));
    }
    
    // 유효성 검사 실패 시 발생하는 MethodArgumentNotValidException 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String errorMessage = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getDefaultMessage()).orElse("잘못된 요청입니다.");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), errorMessage));
    }
    
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "동시 수정 충돌이 발생했습니다."));
    }
}
