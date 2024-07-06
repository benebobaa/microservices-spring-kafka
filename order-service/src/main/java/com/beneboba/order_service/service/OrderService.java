package com.beneboba.order_service.service;

import com.beneboba.order_service.entity.Order;
import com.beneboba.order_service.entity.OrderItem;
import com.beneboba.order_service.exception.OrderAlreadyCancelledException;
import com.beneboba.order_service.exception.OrderNotFoundException;
import com.beneboba.order_service.model.OrderCreateRequest;
import com.beneboba.order_service.model.OrderItemRequest;
import com.beneboba.order_service.model.OrderRequest;
import com.beneboba.order_service.entity.OrderStatus;
import com.beneboba.order_service.repository.OrderRepository;
import com.beneboba.order_service.repository.OrderItemRepository;
import com.beneboba.order_service.util.ObjectConverter;
import com.beneboba.order_service.util.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.saga.OrderEvent;
import org.example.common.saga.SagaEvent;
import org.example.common.saga.SagaEventType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    @Value("${kafka.saga-topic-webclient.topics}")
    String sagaTopic;

    private final OrderRepository orderRepository;

    private final OrderItemRepository orderItemRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ValidationService validationService;

    private final ObjectConverter objectConverter;

    @Transactional
    public Mono<OrderCreateRequest> createOrder(OrderCreateRequest request) {

        request.getOrder().setOrderStatus(OrderStatus.PROCESSING);

        return validationService.validate(request)
                .flatMap(valid -> orderRepository.save(request.getOrder().toEntity()))
                .flatMap(order -> {
                    List<OrderItemRequest> orderItems = request.getProducts();
                    orderItems.forEach(orderItem -> orderItem.setOrderId(order.getId()));

                    request.setProducts(orderItems);
                    request.getOrder().setId(order.getId());
                    request.getOrder().setOrderDate(order.getOrderDate());

                    List<OrderItem> orderItemsEntities = orderItems.stream()
                            .map(OrderItemRequest::toEntity)
                            .toList();

                    return orderItemRepository.saveAll(orderItemsEntities)
                            .collectList()
                            .thenReturn(request);
                })
                .doOnSuccess(savedOrder -> {
                    OrderEvent orderEvent = new OrderEvent(
                            savedOrder.getOrder().toEvent(),
                            savedOrder.getProducts().stream()
                                    .map(OrderItemRequest::toEvent)
                                    .toList()
                    );

                    SagaEvent event = new SagaEvent(UUID.randomUUID().toString(),
                            "Order created, processing products reservation and payment",
                            SagaEventType.ORDER_CREATED, orderEvent);

                    String strEvent = objectConverter.convertObjectToString(event);

                    log.info("Sending ORDER_CREATED event :: {}", strEvent);
                    kafkaTemplate.send(sagaTopic, strEvent);
                });
    }

    @Transactional
    public Mono<OrderCreateRequest> cancelOrderAndRefund(Long orderId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found with id: " + orderId)))
                .flatMap(order -> {
                    if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                        return Mono.error(new OrderAlreadyCancelledException("Order already cancelled: " + orderId));
                    }
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    return orderRepository.save(order);
                })
                .flatMap(cancelledOrder -> orderItemRepository.findByOrderId(orderId).collectList()
                        .map(orderItems -> {
                            OrderCreateRequest cancelledOrderRequest = new OrderCreateRequest();
                            cancelledOrderRequest.setOrder(cancelledOrder.toRequest());
                            cancelledOrderRequest.setProducts(orderItems.stream()
                                    .map(OrderItem::toRequest)
                                    .collect(Collectors.toList()));
                            return cancelledOrderRequest;
                        })
                )
                .doOnSuccess(cancelledOrderRequest -> {
                    OrderEvent orderEvent = new OrderEvent(
                            cancelledOrderRequest.getOrder().toEvent(),
                            cancelledOrderRequest.getProducts().stream()
                                    .map(OrderItemRequest::toEvent)
                                    .toList()
                    );

                    SagaEvent event = new SagaEvent(UUID.randomUUID().toString(),
                            "Order cancelled, processing refund",
                            SagaEventType.ORDER_CANCELLED, orderEvent);

                    String strEvent = objectConverter.convertObjectToString(event);

                    log.info("Sending ORDER_CANCELLED event :: {}", strEvent);

                    kafkaTemplate.send(sagaTopic, strEvent);
                });
    }

    public Mono<Order> updateOrderStatusAmountAndPrice(SagaEvent event) {

        org.example.common.saga.Order orderEvent = event.getOrderRequest().getOrder();

        return orderRepository.findById(orderEvent.getId())
                .flatMap(order -> {
                    order.setTotalAmount(orderEvent.getTotalAmount());
                    order.setOrderStatus(OrderStatus.COMPLETED);
                    return orderRepository.save(order);
                })
                .flatMap(savedOrder ->
                        orderItemRepository.findByOrderId(savedOrder.getId())
                                .collectList()
                                .flatMap(orderItems -> {
                                    orderItems.forEach(orderItem -> {
                                        event.getOrderRequest().getProducts().forEach(product -> {
                                            if (product.getProductId().equals(orderItem.getProductId())) {
                                                orderItem.setPrice(product.getPrice());
                                            }
                                        });
                                    });
                                    return orderItemRepository.saveAll(orderItems).then(Mono.just(savedOrder));
                                })
                )
                .doOnSuccess(order -> log.info("Order updated to Completed :: {}", order));
    }

    public Mono<Void> cancelOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .flatMap(order -> {
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    return orderRepository.save(order);
                })
                .doOnSuccess(
                        order -> {
                            log.info("Order update to Cancelled :: {}", order);
                        }
                )
                .then();
    }

    public Flux<OrderCreateRequest> getAllOrdersWithItsProducts() {
        return orderRepository.findAll()
                .flatMap(order -> {
                    OrderRequest orderRequest = new OrderRequest(order.getId(), order.getCustomerId(),
                            order.getBillingAddress(), order.getShippingAddress(), order.getOrderStatus(),
                             order.getTotalAmount(), order.getPaymentMethod(), order.getOrderDate());
                    return orderItemRepository.findByOrderId(order.getId())
                            .collectList()
                            .map(products -> {
                                        List<OrderItemRequest> productsRequest = products.stream()
                                                .map(
                                                        orderItem -> new OrderItemRequest(
                                                                orderItem.getId(),
                                                                orderItem.getProductId(),
                                                                orderItem.getPrice(),
                                                                orderItem.getQuantity(),
                                                                order.getId()
                                                        )
                                                )
                                                .toList();
                                        return new OrderCreateRequest(orderRequest, productsRequest);
                                    }
                            );
                        }
                );
    }

    public Mono<Void> deleteAllOrders(){
        return orderRepository.deleteAll();
    }
}