package com.beneboba.payment_service.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderItem {

    private Long id;

    private Long productId;

    private float price;

    private Integer quantity;

    private Long orderId;
}
