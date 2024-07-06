package com.beneboba.orchestrator_service.dto.payment;

import lombok.Data;

@Data
public class TransactionRefundRequest {

    private Long orderId;

    private Long customerId;
}
