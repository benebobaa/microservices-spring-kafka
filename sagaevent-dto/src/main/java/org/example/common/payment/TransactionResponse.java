package org.example.common.payment;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
