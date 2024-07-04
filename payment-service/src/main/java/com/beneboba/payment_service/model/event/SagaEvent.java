package com.beneboba.payment_service.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
public class SagaEvent {

    private String sagaId;

    private String message;

    private SagaEventType type;

    private OrderEvent orderRequest;
}
