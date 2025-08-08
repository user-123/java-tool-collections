package org.dc.javatools.exception;

abstract class AssertionException extends IllegalArgumentException {

    public AssertionException() {
        this("斷言為非");
    }

    public AssertionException(String message) {
        super(message);
    }

}
