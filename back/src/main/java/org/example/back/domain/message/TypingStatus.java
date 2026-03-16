package org.example.back.domain.message;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TypingStatus {
    TYPING("typing"),
    STOP("stop");

    private final String value;

    public static Optional<TypingStatus> from(String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst();
    }
}
