package org.example.common;

public enum SagaEventType {
    ORDER_CREATED,
    PRODUCT_RESERVED,
    PRODUCT_RESERVATION_FAILED,
    PAYMENT_PROCESSED,
    PRODUCT_RELEASED,
    PAYMENT_FAILED,
    SAGA_COMPLETED,
    SAGA_FAILED
}