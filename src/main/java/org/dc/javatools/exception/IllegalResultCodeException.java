package org.dc.javatools.exception;

public class IllegalResultCodeException extends IllegalIdException {

    public IllegalResultCodeException() {
        this("未知的result code");
    }

    public IllegalResultCodeException(String message) {
        super(message);
    }

}
