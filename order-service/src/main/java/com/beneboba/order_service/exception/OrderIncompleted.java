package com.beneboba.order_service.exception;

public class OrderIncompleted extends RuntimeException{
    public OrderIncompleted(String message) {
        super(message);
    }
}
