package org.example.back.domain.message;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TypingStatus {
    TYPING("typing"),
    STOP("stop");
    
    private final String value;
    
    public static TypingStatus from(String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("올바르지 않은 타이핑 상태입니다: " + value));
    }
}
