package org.example.back.exception;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String code;
    private String message;
    private LocalDateTime timestamp;
    private String path;

    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(status, code, message, LocalDateTime.now(), path);
    }
}
