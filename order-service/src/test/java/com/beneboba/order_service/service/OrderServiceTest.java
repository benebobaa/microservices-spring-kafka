package com.beneboba.order_service.service;

import com.beneboba.order_service.entity.Order;
import com.beneboba.order_service.entity.OrderItem;
import com.beneboba.order_service.entity.OrderStatus;
import com.beneboba.order_service.exception.OrderAlreadyCancelledException;
import com.beneboba.order_service.exception.OrderIncompleted;
import com.beneboba.order_service.exception.OrderNotFoundException;
import com.beneboba.order_service.model.OrderCreateRequest;
import com.beneboba.order_service.model.OrderItemRequest;
import com.beneboba.order_service.model.OrderRequest;
import com.beneboba.order_service.repository.OrderItemRepository;
import com.beneboba.order_service.repository.OrderRepository;
import com.beneboba.order_service.util.ObjectConverter;
import com.beneboba.order_service.util.ValidationService;
import org.example.common.saga.OrderEvent;
import org.example.common.saga.SagaEvent;
import org.example.common.saga.SagaEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ValidationService validationService;

    @Mock
    private ObjectConverter objectConverter;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderService.sagaTopic = "test-saga-topic";
    }


    @Test
    void testCreateOrder() {
        // Arrange
        OrderCreateRequest request = createSampleOrderCreateRequest();
        Order order = request.getOrder().toEntity();
        order.setId(1L);

        when(validationService.validate(any(OrderCreateRequest.class))).thenReturn(Mono.just(request));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order));
        when(orderItemRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(createSampleOrderItems()));
        when(objectConverter.convertObjectToString(any(SagaEvent.class))).thenReturn("event-string");

        Mono<OrderCreateRequest> result = orderService.createOrder(request);

        StepVerifier.create(result)
                .expectNextMatches(savedRequest -> {
                    assertNotNull(savedRequest.getOrder().getId());
                    assertEquals(OrderStatus.CREATED, savedRequest.getOrder().getOrderStatus());
                    return true;
                })
                .verifyComplete();

        verify(kafkaTemplate, times(1)).send(eq("test-saga-topic"), anyString());
    }

    @Test
    void testUpdateOrderStatusAmountAndPrice() {
        SagaEvent event = createSampleSagaEvent();
        Order order = new Order();
        order.setId(1L);

        when(orderRepository.findById(anyLong())).thenReturn(Mono.just(order));
        when(orderRepository.save(any())).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(anyLong())).thenReturn(Flux.fromIterable(createSampleOrderItems()));
        when(orderItemRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(createSampleOrderItems()));

        StepVerifier.create(orderService.updateOrderStatusAmountAndPrice(event))
                .expectNextMatches(updatedOrder -> updatedOrder.getOrderStatus() == OrderStatus.COMPLETED)
                .verifyComplete();
    }

    @Test
    void testCancelOrder() {
        Order order = new Order();
        order.setId(1L);

        when(orderRepository.findById(anyLong())).thenReturn(Mono.just(order));
        when(orderRepository.save(any())).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.cancelOrder(1L))
                .verifyComplete();

        verify(orderRepository).save(argThat(savedOrder -> savedOrder.getOrderStatus() == OrderStatus.CANCELLED));
    }

    @Test
    void testGetAllOrdersWithItsProducts() {
        Order order1 = createSampleOrder(1L);
        Order order2 = createSampleOrder(2L);
        List<OrderItem> orderItems = createSampleOrderItems();

        when(orderRepository.findAll()).thenReturn(Flux.just(order1, order2));
        when(orderItemRepository.findByOrderId(anyLong())).thenReturn(Flux.fromIterable(orderItems));

        StepVerifier.create(orderService.getAllOrdersWithItsProducts())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void testCancelOrderAndRefund_ShouldCancelOrder() {
        Long orderId = 1L;
        Order order = createSampleOrder(orderId);
        order.setOrderStatus(OrderStatus.COMPLETED);
        List<OrderItem> orderItems = createSampleOrderItems();

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Flux.fromIterable(orderItems));
        when(objectConverter.convertObjectToString(any(SagaEvent.class))).thenReturn("event-string");

        StepVerifier.create(orderService.cancelOrderAndRefund(orderId))
                .expectNextMatches(response -> {
                    assertThat(response.getOrder().getId()).isEqualTo(orderId);
                    assertThat(response.getOrder().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
                    assertThat(response.getProducts().size()).isEqualTo(orderItems.size());
                    return true;
                })
                .verifyComplete();

        verify(orderRepository).save(argThat(savedOrder -> savedOrder.getOrderStatus() == OrderStatus.CANCELLED));
        verify(kafkaTemplate).send(eq("test-saga-topic"), anyString());
    }

    @Test
    void testCancelOrderAndRefund_OrderNotFound() {
        Long orderId = 1L;

        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.cancelOrderAndRefund(orderId))
                .expectErrorMatches(throwable -> throwable instanceof OrderNotFoundException &&
                        throwable.getMessage().equals("Order not found with id: " + orderId))
                .verify();

        verify(orderRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void testCancelOrderAndRefund_OrderAlreadyCancelled() {
        Long orderId = 1L;
        Order order = createSampleOrder(orderId);
        order.setOrderStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.cancelOrderAndRefund(orderId))
                .expectErrorMatches(throwable -> throwable instanceof OrderAlreadyCancelledException &&
                        throwable.getMessage().equals("Order already cancelled: " + orderId))
                .verify();

        verify(orderRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void testCancelOrderAndRefund_OrderIncompleted() {
        Long orderId = 1L;
        Order order = createSampleOrder(orderId);
        order.setOrderStatus(OrderStatus.CREATED); // Incomplete status

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.cancelOrderAndRefund(orderId))
                .expectErrorMatches(throwable -> throwable instanceof OrderIncompleted &&
                        throwable.getMessage().equals("Cannot cancel and refund an incomplete order: " + orderId))
                .verify();

        verify(orderRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void testDeleteAllOrders() {
        when(orderRepository.deleteAll()).thenReturn(Mono.empty());

        StepVerifier.create(orderService.deleteAllOrders())
                .verifyComplete();

        verify(orderRepository).deleteAll();
    }

    private OrderCreateRequest createSampleOrderCreateRequest() {
        OrderRequest orderRequest = new OrderRequest(null, 1L, "Billing Address", "Shipping Address",
                OrderStatus.CREATED, 100.0f, "Credit Card", null);
        List<OrderItemRequest> orderItems = Arrays.asList(
                new OrderItemRequest(null, 1L, 50.0f, 1, null),
                new OrderItemRequest(null, 2L, 50.0f, 1, null)
        );
        return new OrderCreateRequest(orderRequest, orderItems);
    }

    private SagaEvent createSampleSagaEvent() {
        org.example.common.saga.Order orderEvent = new org.example.common.saga.Order();
        orderEvent.setId(1L);
        orderEvent.setTotalAmount(100.0f);
        List<org.example.common.saga.OrderItem> products = Arrays.asList(
                new org.example.common.saga.OrderItem(1L, 1L, 50.0f, 1, 1L),
                new org.example.common.saga.OrderItem(2L, 2L,50.0f, 1, 1L)
        );
        OrderEvent orderEventWrapper = new OrderEvent(orderEvent, products);
        return new SagaEvent(UUID.randomUUID().toString(), "Test event", SagaEventType.ORDER_CREATED, orderEventWrapper);
    }

    private Order createSampleOrder(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setCustomerId(1L);
        order.setBillingAddress("Billing Address");
        order.setShippingAddress("Shipping Address");
        order.setOrderStatus(OrderStatus.CREATED);
        order.setTotalAmount(100f);
        order.setPaymentMethod("Credit Card");
        order.setOrderDate(LocalDate.from(LocalDateTime.now()));
        return order;
    }

    private List<OrderItem> createSampleOrderItems() {
        return Arrays.asList(
                new OrderItem(1L, 1L, 50.0f, 1, 1L),
                new OrderItem(2L, 2L, 50.0f, 1, 1L)
        );
    }
}