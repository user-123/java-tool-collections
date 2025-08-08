package org.dc.javatools.exception;

abstract class UserCheckException extends UserException {

    public UserCheckException() {
        this("用戶檢查例外");
    }

    public UserCheckException(String message) {
        super(message);
    }

}
