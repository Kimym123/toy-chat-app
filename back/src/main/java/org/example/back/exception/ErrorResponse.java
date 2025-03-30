package org.example.back.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    
    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message);
    }
}
