package com.beneboba.product_service.consumer;

import com.beneboba.product_service.producer.ProductProducer;
import com.beneboba.product_service.service.ProductService;
import com.beneboba.product_service.util.ObjectConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.OrderEvent;
import org.example.common.SagaEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEventListener {

    private final ProductProducer productProducer;

    private final ObjectConverter objectConverter;

    @KafkaListener(topics = "${kafka.product-topic.topics}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleSagaEvents(String sagaEvent) {

        log.info("handleSagaEvents :: {}", sagaEvent);

        SagaEvent event = objectConverter.convertStringToObject(sagaEvent, SagaEvent.class);

        switch (event.getType()) {
            case ORDER_CREATED:
                handleOrderCreated(event);
                break;
            case SAGA_FAILED:
                handleSagaFailed(event);
                break;
        }
    }

    private void handleOrderCreated(SagaEvent event) {
        log.info("handleOrderCreated :: event: {}", event);
        OrderEvent payload = event.getOrderRequest();
        productProducer.reserveProducts(event.getSagaId(), payload)
                    .subscribe();
    }

    private void handleSagaFailed(SagaEvent event) {
        log.info("handleSagaFailed :: event {}", event);
        OrderEvent payload = event.getOrderRequest();
        productProducer.releaseProducts(event.getSagaId(), payload)
                .subscribe();
    }
}