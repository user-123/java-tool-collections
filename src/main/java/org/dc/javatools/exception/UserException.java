package org.dc.javatools.exception;

abstract class UserException extends IllegalStateException {

    public UserException() {
        this("用戶例外");
    }

    public UserException(String message) {
        super(message);
    }

}
