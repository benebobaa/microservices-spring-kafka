package com.beneboba.payment_service.model;

import lombok.Data;

@Data
public class TransactionRefundRequest {

    private Long orderId;

    private Long customerId;
}
