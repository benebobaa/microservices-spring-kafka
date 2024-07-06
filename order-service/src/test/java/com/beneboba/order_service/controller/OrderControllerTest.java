package com.beneboba.order_service.controller;

import com.beneboba.order_service.controller.OrderController;
import com.beneboba.order_service.model.OrderCreateRequest;
import com.beneboba.order_service.model.OrderItemRequest;
import com.beneboba.order_service.model.OrderRequest;
import com.beneboba.order_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        webTestClient = WebTestClient.bindToController(orderController).build();
    }

    @Test
    void getAllWithProducts_ShouldReturnOrders() {
        OrderCreateRequest order1 = createSampleOrder(1L);
        OrderCreateRequest order2 = createSampleOrder(2L);

        when(orderService.getAllOrdersWithItsProducts()).thenReturn(Flux.just(order1, order2));

        webTestClient.get()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OrderCreateRequest.class)
                .value(responseList -> {
                    for (int i = 0; i < responseList.size(); i++) {
                        OrderCreateRequest expected = i == 0 ? order1 : order2;
                        OrderCreateRequest actual = responseList.get(i);
                        assertThat(actual.getOrder().getCustomerId()).isEqualTo(expected.getOrder().getCustomerId());
                        assertThat(actual.getOrder().getBillingAddress()).isEqualTo(expected.getOrder().getBillingAddress());
                        assertThat(actual.getOrder().getShippingAddress()).isEqualTo(expected.getOrder().getShippingAddress());
                        assertThat(actual.getOrder().getPaymentMethod()).isEqualTo(expected.getOrder().getPaymentMethod());
                        assertThat(actual.getProducts().getFirst().getProductId()).isEqualTo(expected.getProducts().getFirst().getProductId());
                        assertThat(actual.getProducts().getFirst().getQuantity()).isEqualTo(expected.getProducts().getFirst().getQuantity());
                    }
                });

        verify(orderService, times(1)).getAllOrdersWithItsProducts();
    }

    @Test
    void createOrder_ShouldCreateAndReturnOrder() {
        OrderCreateRequest orderRequest = createSampleOrder(null); // Change to null
        OrderCreateRequest expectedResponse = createSampleOrder(null); // Change to null

        when(orderService.createOrder(any(OrderCreateRequest.class))).thenReturn(Mono.just(expectedResponse));

        webTestClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OrderCreateRequest.class)
                .value(response -> {
                    assertThat(response.getOrder().getId()).isNull();
                    assertThat(response.getOrder().getCustomerId()).isEqualTo(expectedResponse.getOrder().getCustomerId());
                    assertThat(response.getOrder().getBillingAddress()).isEqualTo(expectedResponse.getOrder().getBillingAddress());
                    assertThat(response.getOrder().getShippingAddress()).isEqualTo(expectedResponse.getOrder().getShippingAddress());
                    assertThat(response.getOrder().getPaymentMethod()).isEqualTo(expectedResponse.getOrder().getPaymentMethod());
                    assertThat(response.getProducts().getFirst().getId()).isNull();
                    assertThat(response.getProducts().getFirst().getProductId()).isEqualTo(expectedResponse.getProducts().getFirst().getProductId());
                });

        verify(orderService, times(1)).createOrder(any(OrderCreateRequest.class));
    }

    @Test
    void deleteAll_ShouldDeleteAllOrders() {
        when(orderService.deleteAllOrders()).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isNoContent();

        verify(orderService, times(1)).deleteAllOrders();
    }

    @Test
    void cancelOrder_ShouldCancelAndReturnOrder() {
        Long orderId = 1L;
        OrderCreateRequest expectedResponse = createSampleOrder(orderId);

        when(orderService.cancelOrderAndRefund(orderId)).thenReturn(Mono.just(expectedResponse));

        webTestClient.patch()
                .uri("/api/orders/{orderId}", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderCreateRequest.class)
                .value(response -> {
                    assertThat(response.getOrder().getCustomerId()).isEqualTo(expectedResponse.getOrder().getCustomerId());
                    assertThat(response.getOrder().getBillingAddress()).isEqualTo(expectedResponse.getOrder().getBillingAddress());
                    assertThat(response.getOrder().getShippingAddress()).isEqualTo(expectedResponse.getOrder().getShippingAddress());
                    assertThat(response.getOrder().getPaymentMethod()).isEqualTo(expectedResponse.getOrder().getPaymentMethod());
                    assertThat(response.getProducts().getFirst().getProductId()).isEqualTo(expectedResponse.getProducts().getFirst().getProductId());
                    assertThat(response.getProducts().getFirst().getQuantity()).isEqualTo(expectedResponse.getProducts().getFirst().getQuantity());
                });

        verify(orderService, times(1)).cancelOrderAndRefund(orderId);
    }


    private OrderCreateRequest createSampleOrder(Long id) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setId(id);
        orderRequest.setCustomerId(1L);
        orderRequest.setBillingAddress("123 Billing St");
        orderRequest.setShippingAddress("456 Shipping Ave");
        orderRequest.setPaymentMethod("BCA");

        OrderItemRequest orderItemRequest = new OrderItemRequest(null, 1L, 10.0f, 2, id);

        OrderCreateRequest orderCreateRequest = new OrderCreateRequest();
        orderCreateRequest.setOrder(orderRequest);
        orderCreateRequest.setProducts(List.of(orderItemRequest));

        return orderCreateRequest;
    }
}