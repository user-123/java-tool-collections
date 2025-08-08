package org.dc.javatools.exception;

public class IllegalIdException extends ArgumentCheckException {

    public IllegalIdException() {
        this("id不存在或不合法");
    }

    public IllegalIdException(String message) {
        super(message);
    }

}
