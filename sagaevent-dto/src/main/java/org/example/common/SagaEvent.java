package org.example.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class SagaEvent {

    private String sagaId;

    private String message;

    private SagaEventType type;

    private OrderEvent orderRequest;
}
