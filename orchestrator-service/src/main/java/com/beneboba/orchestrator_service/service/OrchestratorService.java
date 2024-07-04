package com.beneboba.orchestrator_service.service;

import com.beneboba.orchestrator_service.event.*;
import com.beneboba.orchestrator_service.event.order.OrderEvent;
import com.beneboba.orchestrator_service.util.ObjectConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectConverter objectConverter;

    public void handleOrderCreated(SagaEvent event) {
        log.info("handleOrderCreated :: {}", event);

        String eventStr = objectConverter.convertObjectToString(event);

        log.info("Sending message to PRODUCT-TOPIC :: {}", eventStr);
        kafkaTemplate.send("product-topic", eventStr);
    }

    public void handleProductReserved(SagaEvent event) {
        log.info("handleProductReserved :: {}", event);

        String eventStr = objectConverter.convertObjectToString(event);

        log.info("Sending event to PAYMENT-TOPIC :: {}", eventStr);
        kafkaTemplate.send("payment-topic", eventStr);
    }

    public void handleProductReservationFailed(SagaEvent event) {
        log.info("handleProductReservationFailed event:: {}", event);

        cancelOrder(event.getSagaId(), event.getMessage(), event.getOrderRequest());
    }

    public void handlePaymentProcessed(SagaEvent event) {
        log.info("handlePaymentProcessed :: {}", event);

        SagaEvent completeSagaOrder = new SagaEvent(event.getSagaId(), event.getMessage(),
                SagaEventType.SAGA_COMPLETED, event.getOrderRequest());

        String eventStr = objectConverter.convertObjectToString(completeSagaOrder);

        log.info("Sending complete saga order event :: {}", eventStr);
        kafkaTemplate.send("order-topic", eventStr);
    }

    public void handlePaymentFailed(SagaEvent event) {
        log.info("handlePaymentFailed :: {}", event);

        cancelOrder(event.getSagaId(), event.getMessage(), event.getOrderRequest());
        releaseProductReservations(event);
    }

    public void cancelOrder(String sagaId, String message, OrderEvent order) {
        log.info("cancel order");

        SagaEvent cancelOrderEvent = new SagaEvent(sagaId, message,
                SagaEventType.SAGA_FAILED, order);

        String eventStr = objectConverter.convertObjectToString(cancelOrderEvent);
        log.info("Sending cancel order :: {}", eventStr);
        kafkaTemplate.send("order-topic", eventStr);
    }

    public void releaseProductReservations(SagaEvent event) {
        log.info("releaseProductReservations :: {}", event);

        SagaEvent releaseReservationsEvent = new SagaEvent(event.getSagaId(), event.getMessage(),
                SagaEventType.SAGA_FAILED, event.getOrderRequest());

        kafkaTemplate.send("product-topic",
                objectConverter.convertObjectToString(releaseReservationsEvent));
    }
}