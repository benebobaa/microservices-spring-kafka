package com.beneboba.order_service.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.beneboba.order_service.service.OrderService;
import com.beneboba.order_service.util.ObjectConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.common.SagaEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        SagaEvent sagaCompletedEvent = objectConverter.convertStringToObject(sagaCompletedEventJson, SagaEvent.class);

        log.info("sagaCompletedEvent: {}", sagaCompletedEvent);

        when(objectConverter.convertStringToObject(anyString(), eq(SagaEvent.class))).thenReturn(sagaCompletedEvent);
        when(orderService.updateOrderStatusAmountAndPrice(any(SagaEvent.class))).thenReturn(Mono.empty());

        orderEventListener.handleOrderEvents(sagaCompletedEventJson);

        verify(orderService, times(1)).updateOrderStatusAmountAndPrice(any(SagaEvent.class));
    }

    @Test
    void testHandleOrderEvents_SagaFailed() throws Exception {
        String sagaFailedEventJson = readJson("json/saga_failed_event.json");
        log.info("sagaFailedEventJson: {}", sagaFailedEventJson);

        SagaEvent sagaFailedEvent = objectConverter.convertStringToObject(sagaFailedEventJson, SagaEvent.class);

        log.info("sagaFailedEvent: {}", sagaFailedEvent);

        when(objectConverter.convertStringToObject(anyString(), eq(SagaEvent.class))).thenReturn(sagaFailedEvent);
        when(orderService.cancelOrder(anyLong())).thenReturn(Mono.empty());

        orderEventListener.handleOrderEvents(sagaFailedEventJson);

        verify(orderService, times(1)).cancelOrder(1L);
    }

}
