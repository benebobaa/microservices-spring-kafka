package com.beneboba.order_service.model;


import com.beneboba.order_service.entity.Order;
import com.beneboba.order_service.entity.OrderItem;
import com.beneboba.order_service.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    @Min(1)
    private Long customerId;

    @NotBlank
    @Size(min = 3, max = 255)
    private String billingAddress;

    @NotBlank
    @Size(min = 3, max = 255)
    private String shippingAddress;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private OrderStatus orderStatus;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private float totalAmount;

    @NotBlank
    @Pattern(regexp = "(?i)^(BCA|BRI|BNI|GOPAY|OVO|DANA)$", message = "Invalid payment method, only support BCA, BRI, BNI, GOPAY, OVO, DANA")
    private String paymentMethod;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    public Order toEntity(){
        return new Order(
                this.id,
                this.customerId,
                this.billingAddress,
                this.shippingAddress,
                this.orderStatus,
                this.paymentMethod,
                0,
                this.orderDate
        );
    }

    public org.example.common.saga.Order toEvent(){
        return new org.example.common.saga.Order(
                this.id,
                this.customerId,
                this.billingAddress,
                this.shippingAddress,
                org.example.common.saga.OrderStatus.PROCESSING,
                this.paymentMethod,
                0,
                this.orderDate
        );
    }
}

