package org.example.back.exception.auth;

import org.example.back.exception.base.CustomException;

public class AuthException extends CustomException {
    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
