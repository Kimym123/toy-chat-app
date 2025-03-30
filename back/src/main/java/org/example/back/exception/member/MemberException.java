package org.example.back.exception.member;

import org.example.back.exception.base.CustomException;

public class MemberException extends CustomException {
    public MemberException(MemberErrorCode errorCode) {
        super(errorCode);
    }
}
