package org.dc.javatools.exception;

public class IllegalTypeException extends ArgumentCheckException {

    public IllegalTypeException() {
        this("型別不存在或不合法");
    }

    public IllegalTypeException(String message) {
        super(message);
    }

}
