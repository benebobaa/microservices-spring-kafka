package com.beneboba.payment_service.model.event;

public enum SagaEventType {
    ORDER_CREATED,
    PRODUCT_RESERVED,
    PRODUCT_RESERVATION_FAILED,
    PAYMENT_PROCESSED,
    PAYMENT_FAILED,
    SAGA_COMPLETED,
    SAGA_FAILED
}