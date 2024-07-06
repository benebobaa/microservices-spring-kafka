package com.beneboba.product_service.producer;

import com.beneboba.product_service.entity.Product;
import com.beneboba.product_service.repository.ProductRepository;
import com.beneboba.product_service.util.ObjectConverter;
import org.example.common.Order;
import org.example.common.OrderEvent;
import org.example.common.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductProducerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectConverter objectConverter;

    @InjectMocks
    private ProductProducer productProducer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productProducer.sagaTopic = "test-saga-topic";
    }

    @Test
    void testReserveProductsSuccess() {
        String sagaId = "saga-123";
        OrderEvent orderEvent = createSampleOrderEvent();
        Product product1 = new Product(1L, "Product 1", 10.0f,null,null, null, 5, null, null);
        Product product2 = new Product(2L, "Product 2", 20.0f,null,null, null, 3, null, null);

        when(productRepository.findById(1L)).thenReturn(Mono.just(product1));
        when(productRepository.findById(2L)).thenReturn(Mono.just(product2));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product1)).thenReturn(Mono.just(product2));
        when(objectConverter.convertObjectToString(any())).thenReturn("event-string");

        StepVerifier.create(productProducer.reserveProducts(sagaId, orderEvent))
                .verifyComplete();

        verify(kafkaTemplate).send(eq("test-saga-topic"), anyString());
    }

    @Test
    void testReserveProductsNotEnoughStock() {
        String sagaId = "saga-123";
        OrderEvent orderEvent = createSampleOrderEvent();
        Product product = new Product(1L, "Product 1", 10.0f,null,null, null, 0, null, null);

        when(productRepository.findById(1L)).thenReturn(Mono.just(product));
        when(productRepository.findById(2L)).thenReturn(Mono.just(product));
        when(objectConverter.convertObjectToString(any())).thenReturn("event-string");

        StepVerifier.create(productProducer.reserveProducts(sagaId, orderEvent))
                .verifyError();

        verify(kafkaTemplate).send(eq("test-saga-topic"), anyString());
    }

    @Test
    void testReserveProductsProductNotFound() {
        String sagaId = "saga-123";
        OrderEvent orderEvent = createSampleOrderEvent();

        when(productRepository.findById(1L)).thenReturn(Mono.empty());
        when(objectConverter.convertObjectToString(any())).thenReturn("event-string");

        StepVerifier.create(productProducer.reserveProducts(sagaId, orderEvent))
                .verifyError();

        verify(kafkaTemplate).send(eq("test-saga-topic"), anyString());
    }

    @Test
    void testReleaseProducts() {
        String sagaId = "saga-123";
        OrderEvent orderEvent = createSampleOrderEvent();
        Product product1 = new Product(1L, "Product 1", 10.0f,null,null, null, 5, null, null);
        Product product2 = new Product(2L, "Product 2", 20.0f,null,null, null, 3, null, null);

        when(productRepository.findById(1L)).thenReturn(Mono.just(product1));
        when(productRepository.findById(2L)).thenReturn(Mono.just(product2));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product1)).thenReturn(Mono.just(product2));
        when(objectConverter.convertObjectToString(any())).thenReturn("event-string");

        StepVerifier.create(productProducer.releaseProducts(sagaId, orderEvent))
                .verifyComplete();

        verify(kafkaTemplate).send(eq("test-saga-topic"), anyString());
    }

    private OrderEvent createSampleOrderEvent() {
        Order order = new Order();
        order.setId(1L);
        order.setTotalAmount(0.0f);

        OrderItem product1 = new OrderItem(1L, 2L, 0.0f, 1, 1L);
        OrderItem product2 = new OrderItem(2L, 1L, 0.0f, 1,1L);
        List<OrderItem> products = Arrays.asList(product1, product2);

        OrderEvent orderEvent = new OrderEvent();
        orderEvent.setOrder(order);
        orderEvent.setProducts(products);

        return orderEvent;
    }
}