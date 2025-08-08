package org.dc.javatools.exception;

public class ApiException extends IllegalStateException {

    public ApiException() {
        this("api異常");
    }

    public ApiException(String message) {
        super(message);
    }

}
