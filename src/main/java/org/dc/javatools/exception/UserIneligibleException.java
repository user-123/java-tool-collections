package org.dc.javatools.exception;

public class UserIneligibleException extends UserCheckException {

    public UserIneligibleException() {
        this("用戶資格不符");
    }

    public UserIneligibleException(String message) {
        super(message);
    }

}
