package org.example.common.saga;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SagaEvent {

    private String sagaId;

    private String message;

    private SagaEventType type;

    private OrderEvent orderRequest;
}
