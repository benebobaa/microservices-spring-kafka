package org.example.common.saga;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SagaResult {

    private String sagaId;

    private Long OrderId;

    private SagaEventType type;
}
