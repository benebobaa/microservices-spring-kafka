package com.beneboba.payment_service.exception;

public class CustomerIdNotMatchException extends RuntimeException{
    public CustomerIdNotMatchException(String message) {
        super(message);
    }
}
