package com.beneboba.orchestrator_service.dto.payment;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TransactionResponse {
    private Long id;

    private Long orderId;

    private float amount;

    private String mode;

    private PaymentStatus status;

    private String referenceNumber;
}
