package org.example.common.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TransactionRequest {

    private Long orderId;

    private Long customerId;

    private float amount;

    private String paymentMethod;
}
