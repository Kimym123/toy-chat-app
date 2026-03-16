package org.example.back.exception.file;

import org.example.back.exception.base.CustomException;

public class FileException extends CustomException {

    public FileException(FileErrorCode errorCode) {
        super(errorCode);
    }

    public FileException(FileErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
