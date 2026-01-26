package com.wells.bill.assistant.exception;

public class InvalidContextException extends RuntimeException {
    public InvalidContextException(String noContextFound) {
        super(noContextFound);
    }
}
