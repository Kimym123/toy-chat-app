package org.example.back.dto.websocket.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StompErrorResponse {
    private String code;
    private String message;
    private LocalDateTime timestamp;

    public static StompErrorResponse of(String code, String message) {
        return new StompErrorResponse(code, message, LocalDateTime.now());
    }
}
