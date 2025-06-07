package com.work.exception;

public class InvalidLoanAmountException extends RuntimeException{
    public InvalidLoanAmountException(String message) {
        super(message);
    }
}
