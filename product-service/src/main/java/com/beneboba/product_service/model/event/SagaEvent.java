package com.beneboba.product_service.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.annotation.Order;

import java.util.List;

@AllArgsConstructor
@Data
public class SagaEvent {

    private String sagaId;

    private String message;

    private SagaEventType type;

    private OrderEvent orderRequest;
}
