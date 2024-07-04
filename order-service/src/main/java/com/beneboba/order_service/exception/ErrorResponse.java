package com.beneboba.order_service.exception;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ErrorResponse {
    private String message;
    private Integer statusCode;

    public ErrorResponse(String message, Integer statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
}