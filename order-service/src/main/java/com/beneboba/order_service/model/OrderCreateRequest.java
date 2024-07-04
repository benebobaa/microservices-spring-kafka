package com.beneboba.order_service.model;

import com.beneboba.order_service.entity.Order;
import com.beneboba.order_service.entity.OrderItem;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderCreateRequest {

    @Valid
    private OrderRequest order;

    @Valid
    private List<OrderItemRequest> products;
}
