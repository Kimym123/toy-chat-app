package org.example.back.exception.file;

import lombok.Getter;
import org.example.back.exception.base.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum FileErrorCode implements ErrorCode {

    FILENAME_MISSING(HttpStatus.BAD_REQUEST, "파일명이 존재하지 않습니다."),
    EXTENSION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 확장자입니다."),
    FILE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    FileErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
