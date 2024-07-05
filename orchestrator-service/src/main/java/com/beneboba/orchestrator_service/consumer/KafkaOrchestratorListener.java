package com.beneboba.orchestrator_service.consumer;

import com.beneboba.orchestrator_service.producer.KafkaOrchestratorProducer;
import com.beneboba.orchestrator_service.service.OrchestratorService;
import com.beneboba.orchestrator_service.util.ObjectConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.SagaEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaOrchestratorListener {

    private final KafkaOrchestratorProducer kafkaOrchestratorProducer;

    private final ObjectConverter objectConverter;

    private final OrchestratorService orchestratorService;

//    @KafkaListener(topics = "${kafka.saga-topic.topics}",
//            groupId = "${spring.kafka.consumer.group-id}")
//    public void handleSagaEvents(String sagaEvent) {
//
//        SagaEvent event = objectConverter.convertStringToObject(sagaEvent, SagaEvent.class);
//        log.info("handleSagaEvents :: {}", event);
//
//        switch (event.getType()) {
//            case ORDER_CREATED:
//                kafkaOrchestratorProducer.handleOrderCreated(event);
//                break;
//            case PRODUCT_RESERVED:
//                kafkaOrchestratorProducer.handleProductReserved(event);
//                break;
//            case PRODUCT_RESERVATION_FAILED:
//                kafkaOrchestratorProducer.handleProductReservationFailed(event);
//                break;
//            case PAYMENT_PROCESSED:
//                kafkaOrchestratorProducer.handlePaymentProcessed(event);
//                break;
//            case PAYMENT_FAILED:
//                kafkaOrchestratorProducer.handlePaymentFailed(event);
//                break;
//            case PRODUCT_RELEASED:
//                break;
//            default:
//                throw new IllegalStateException("Unexpected event type: " + event.getType());
//        }
//    }

    @KafkaListener(
            topics = "${kafka.saga-topic-webclient.topics}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleOrderCreated(String sagaEvent){

        log.info("handleOrderCreated :: {}", sagaEvent);

        SagaEvent event = objectConverter.
                convertStringToObject(sagaEvent, SagaEvent.class);

        orchestratorService.startProcessOrder(event)
                .subscribe();
    }
}
