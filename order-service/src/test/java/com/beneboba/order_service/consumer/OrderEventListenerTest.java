package com.beneboba.order_service.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.beneboba.order_service.service.OrderService;
import com.beneboba.order_service.util.ObjectConverter;
import lombok.extern.slf4j.Slf4j;
import org.example.common.saga.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
@EmbeddedKafka(partitions = 1, topics = {"order-topic"})
@Slf4j
public class OrderEventListenerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ObjectConverter objectConverter;

    @InjectMocks
    private OrderEventListener orderEventListener;

    public static String readJson(String filePath) throws Exception {
        ClassPathResource resource = new ClassPathResource(filePath);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    @Test
    void testHandleOrderEvents_SagaCompleted() throws Exception {
        String sagaCompletedEventJson = readJson("json/saga_completed_event.json");

        SagaEvent event = newSagaEvent();

        log.info("sagaCompletedEvent: {}", event);

        when(objectConverter.convertStringToObject(anyString(), eq(SagaEvent.class))).thenReturn(event);
        when(orderService.updateOrderStatusAmountAndPrice(any(SagaEvent.class))).thenReturn(Mono.empty());

        orderEventListener.handleOrderEvents(sagaCompletedEventJson);

        verify(orderService, times(1)).updateOrderStatusAmountAndPrice(any(SagaEvent.class));
    }

    @Test
    void testHandleOrderEvents_SagaFailed() throws Exception {
        String sagaFailedEventJson = readJson("json/saga_failed_event.json");
        log.info("sagaFailedEventJson: {}", sagaFailedEventJson);

        SagaEvent event = newSagaEvent();
        event.setType(SagaEventType.SAGA_FAILED);

        log.info("sagaFailedEvent: {}", event);

        when(objectConverter.convertStringToObject(anyString(), eq(SagaEvent.class))).thenReturn(event);
        when(orderService.cancelOrder(anyLong())).thenReturn(Mono.empty());

        orderEventListener.handleOrderEvents(sagaFailedEventJson);

        verify(orderService, times(1)).cancelOrder(1L);
    }

     private SagaEvent newSagaEvent(){
         OrderItem product1 = new OrderItem();
         product1.setId(null);
         product1.setProductId(2L);
         product1.setPrice(200.0f);
         product1.setQuantity(1);
         product1.setOrderId(1L);

         OrderItem product2 = new OrderItem();
         product2.setId(null);
         product2.setProductId(1L);
         product2.setPrice(100.0f);
         product2.setQuantity(1);
         product2.setOrderId(1L);

         // Create Order object
         Order order = new org.example.common.saga.Order();
         order.setId(1L);
         order.setCustomerId(2L);
         order.setBillingAddress("bene");
         order.setShippingAddress("string");
         order.setOrderStatus(OrderStatus.PROCESSING);
         order.setPaymentMethod("dana");
         order.setTotalAmount(300.0f);
         order.setOrderDate(LocalDate.of(2024, 7, 5));

         // Create OrderRequest object
         OrderEvent orderRequest = new OrderEvent();
         orderRequest.setOrder(order);
         orderRequest.setProducts(Arrays.asList(product1, product2));

         // Create SagaEvent object
         SagaEvent sagaEvent = new SagaEvent();
         sagaEvent.setSagaId("a583b847-3b4f-4f72-b015-7a4cc9b1b790");
         sagaEvent.setMessage("Order completed successfully");
         sagaEvent.setType(SagaEventType.SAGA_COMPLETED);
         sagaEvent.setOrderRequest(orderRequest);

         return sagaEvent;
     }
}
