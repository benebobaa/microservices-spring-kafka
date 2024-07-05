package com.beneboba.payment_service.model;

import com.beneboba.payment_service.entity.PaymentStatus;
import com.beneboba.payment_service.entity.Transaction;
import com.beneboba.payment_service.util.Helper;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TransactionRequest {

    @NotNull
    private Long orderId;

    @NotNull
    private Long customerId;

    @Min(1)
    private float amount;

    @NotNull
    @Pattern(regexp = "(?i)^(BCA|BRI|BNI|GOPAY|OVO|DANA)$", message = "Invalid payment method, only support BCA, BRI, BNI, GOPAY, OVO, DANA")
    private String paymentMethod;

    public Transaction toEntity(){
        return new Transaction(
                null,
                this.orderId,
                this.amount,
                Helper.classifyPaymentMethod(this.paymentMethod),
                PaymentStatus.COMPLETED,
                Helper.generateReferenceNumber(),
                null
        );
    }
}
