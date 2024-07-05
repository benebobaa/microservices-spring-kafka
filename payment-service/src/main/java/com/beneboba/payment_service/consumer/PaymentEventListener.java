package com.beneboba.payment_service.consumer;

import com.beneboba.payment_service.service.PaymentService;
import com.beneboba.payment_service.util.ObjectConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.SagaEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentService paymentService;

    private final ObjectConverter objectConverter;

    @KafkaListener(topics = "${kafka.payment-topic.topics}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentEvents(String sagaEvent) {
        log.info("handlePaymentEvents :: {}", sagaEvent);

        SagaEvent event = objectConverter.convertStringToObject(sagaEvent, SagaEvent.class);

        log.info("payment-topic :: {}", event);

        paymentService.processPayment(event)
                .subscribe(
                        result -> System.out.println("Payment processed successfully: " + result),
                        error -> System.err.println("Error processing payment: " + error.getMessage())
                );
    }
}