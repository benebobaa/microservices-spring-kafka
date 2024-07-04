package com.beneboba.product_service.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class OrderEvent {

    private Order order;

    private List<OrderItem> products;
}
