package com.beneboba.product_service.exception;

public class StockNotEnoughException extends RuntimeException{
    public StockNotEnoughException(String message) {
        super(message);
    }
}
