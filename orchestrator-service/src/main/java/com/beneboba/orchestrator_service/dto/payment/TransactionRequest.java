package com.beneboba.orchestrator_service.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TransactionRequest {

    private Long orderId;

    private Long customerId;

    private float amount;

    private String paymentMethod;
}
