package org.example.back.exception.global;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.back.exception.ErrorResponse;
import org.example.back.exception.base.CustomException;
import org.example.back.exception.base.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        if (errorCode.getStatus().is5xxServerError()) {
            log.error("[{}] {} {} - {}", errorCode.name(), request.getMethod(), request.getRequestURI(), errorCode.getMessage(), exception);
        } else {
            log.warn("[{}] {} {} - {}", errorCode.name(), request.getMethod(), request.getRequestURI(), errorCode.getMessage());
        }
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode.getStatus().value(), errorCode.name(), errorCode.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String errorMessage = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getDefaultMessage()).orElse("잘못된 요청입니다.");
        log.warn("[VALIDATION_ERROR] {} {} - {}", request.getMethod(), request.getRequestURI(), errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", errorMessage, request.getRequestURI()));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockException exception, HttpServletRequest request) {
        log.warn("[OPTIMISTIC_LOCK] {} {} - 동시 수정 충돌 발생", request.getMethod(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "OPTIMISTIC_LOCK", "동시 수정 충돌이 발생했습니다.", request.getRequestURI()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException exception, HttpServletRequest request) {
        log.warn("[ENTITY_NOT_FOUND] {} {} - {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), "ENTITY_NOT_FOUND", exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        log.warn("[ACCESS_DENIED] {} {} - {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED", "접근 권한이 없습니다.", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        log.error("[UNHANDLED] {} {} - {}", request.getMethod(), request.getRequestURI(), exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "UNHANDLED_ERROR", "서버 내부 오류가 발생했습니다.", request.getRequestURI()));
    }
}
