package com.beneboba.orchestrator_service.event;

public enum SagaEventType {
    ORDER_CREATED,
    PRODUCT_RESERVED,
    PRODUCT_RESERVATION_FAILED,
    PRODUCT_RELEASED,
    PAYMENT_PROCESSED,
    PAYMENT_FAILED,
    SAGA_COMPLETED,
    SAGA_FAILED
}