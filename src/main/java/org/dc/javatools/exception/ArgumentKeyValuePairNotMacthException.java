package org.dc.javatools.exception;

public class ArgumentKeyValuePairNotMacthException extends ArgumentNotMacthException {

    public ArgumentKeyValuePairNotMacthException() {
        this("參數key/value不匹配");
    }

    public ArgumentKeyValuePairNotMacthException(String message) {
        super(message);
    }

}
