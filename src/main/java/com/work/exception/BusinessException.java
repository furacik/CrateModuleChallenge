package com.work.exception;

public class BusinessException extends  RuntimeException{
    public BusinessException(String message) {
        super(message);
    }
}
