package com.beneboba.order_service.model.event;

import com.beneboba.order_service.entity.Order;
import com.beneboba.order_service.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class OrderEvent {

    private Order order;

    private List<OrderItem> products;
}