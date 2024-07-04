package com.beneboba.payment_service.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private Long id;

    private Long customerId;

    private String billingAddress;

    private String shippingAddress;

    private OrderStatus orderStatus;

    private String paymentMethod;

    private float totalAmount;

    private LocalDate orderDate;
}
