package com.beneboba.order_service.consumer;

import com.beneboba.order_service.service.OrderService;
import com.beneboba.order_service.util.ObjectConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.saga.SagaEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final OrderService orderService;

    private final ObjectConverter objectConverter;

    @KafkaListener(topics = "${kafka.order-topic.topics}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderEvents(String sagaEvent) {
        log.info("handleOrderEvents :: {}", sagaEvent);

        SagaEvent event = objectConverter.convertStringToObject(sagaEvent, SagaEvent.class);

        log.info("handleOrderEvents obj :: {}", event);

        switch (event.getType()) {
            case SAGA_COMPLETED:
                orderService.updateOrderStatusAmountAndPrice(event).subscribe();
                break;
            case SAGA_FAILED:
                orderService.cancelOrder(event.getOrderRequest().getOrder().getId()).subscribe();
                break;
        }
    }
}