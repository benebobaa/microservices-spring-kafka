package com.beneboba.order_service.exception;

public class OrderAlreadyCancelledException extends RuntimeException{
    public OrderAlreadyCancelledException(String message) {
        super(message);
    }
}
