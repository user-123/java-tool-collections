package org.dc.javatools.exception;

public class ArgumentNotFoundException extends ArgumentCheckException {

    public ArgumentNotFoundException() {
        this("參數缺失或不匹配");
    }

    public ArgumentNotFoundException(String message) {
        super(message);
    }

}
