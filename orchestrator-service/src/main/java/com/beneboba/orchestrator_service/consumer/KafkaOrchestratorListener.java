package com.beneboba.orchestrator_service.consumer;

import com.beneboba.orchestrator_service.event.SagaEvent;
import com.beneboba.orchestrator_service.service.OrchestratorService;
import com.beneboba.orchestrator_service.util.ObjectConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaOrchestratorListener {

    private final OrchestratorService orchestratorService;

    private final ObjectConverter objectConverter;

    @KafkaListener(topics = "saga-topic", groupId = "bene-group")
    public void handleSagaEvents(String sagaEvent) {

        SagaEvent event = objectConverter.convertStringToObject(sagaEvent, SagaEvent.class);
        log.info("handleSagaEvents :: {}", event);

        switch (event.getType()) {
            case ORDER_CREATED:
                orchestratorService.handleOrderCreated(event);
                break;
            case PRODUCT_RESERVED:
                orchestratorService.handleProductReserved(event);
                break;
            case PRODUCT_RESERVATION_FAILED:
                orchestratorService.handleProductReservationFailed(event);
                break;
            case PAYMENT_PROCESSED:
                orchestratorService.handlePaymentProcessed(event);
                break;
            case PAYMENT_FAILED:
                orchestratorService.handlePaymentFailed(event);
                break;
            case PRODUCT_RELEASED:
                break;
            default:
                throw new IllegalStateException("Unexpected event type: " + event.getType());
        }
    }
}
