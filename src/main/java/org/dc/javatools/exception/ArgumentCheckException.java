package org.dc.javatools.exception;

abstract class ArgumentCheckException extends AssertionException {

    public ArgumentCheckException() {
        this("參數錯誤或查無參數");
    }

    public ArgumentCheckException(String message) {
        super(message);
    }

}
