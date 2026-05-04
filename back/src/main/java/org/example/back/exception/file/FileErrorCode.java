package org.example.back.exception.file;

import lombok.Getter;
import org.example.back.exception.base.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum FileErrorCode implements ErrorCode {

    FILENAME_MISSING(HttpStatus.BAD_REQUEST, "파일명이 존재하지 않습니다."),
    EXTENSION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 확장자입니다."),
    FILE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    NO_DOWNLOAD_PERMISSION(HttpStatus.FORBIDDEN, "이 파일을 다운로드할 권한이 없습니다."),
    FILE_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 읽는 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    FileErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
