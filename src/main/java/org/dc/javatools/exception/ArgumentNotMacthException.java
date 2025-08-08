package org.dc.javatools.exception;

public class ArgumentNotMacthException extends ArgumentNotFoundException {

    public ArgumentNotMacthException() {
        this("參數缺失或不匹配");
    }

    public ArgumentNotMacthException(String message) {
        super(message);
    }

}
