package com.biblionet.loanservice.exception;

public class InvalidLoanStateException extends RuntimeException {

    public InvalidLoanStateException(String message) {
        super(message);
    }

}
