package com.beneboba.order_service.controller;

import com.beneboba.order_service.entity.Order;
import com.beneboba.order_service.model.OrderCreateRequest;
import com.beneboba.order_service.model.OrderRequest;
import com.beneboba.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public Flux<OrderCreateRequest> getAllWithProducts() {
        return orderService.getAllOrdersWithItsProducts();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OrderCreateRequest> createOrder(@RequestBody OrderCreateRequest order) {
        return orderService.createOrder(order);
    }

    @PatchMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<OrderCreateRequest> cancelOrder(@PathVariable Long orderId) {
        return orderService.cancelOrderAndRefund(orderId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAll(){
        return orderService.deleteAllOrders();
    }
}