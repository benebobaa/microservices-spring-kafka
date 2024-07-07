package com.beneboba.payment_service.producer;

import com.beneboba.payment_service.entity.PaymentStatus;
import com.beneboba.payment_service.entity.Transaction;
import com.beneboba.payment_service.exception.CustomerNotFoundException;
import com.beneboba.payment_service.exception.InsufficientFundsException;
import com.beneboba.payment_service.repository.BalanceRepository;
import com.beneboba.payment_service.repository.TransactionRepository;
import com.beneboba.payment_service.util.Helper;
import com.beneboba.payment_service.util.ObjectConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.saga.Order;
import org.example.common.saga.SagaEvent;
import org.example.common.saga.SagaEventType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProducer {

    @Value("${kafka.saga-topic.topics}")
    private String sagaTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectConverter objectConverter;

    private final BalanceRepository balanceRepository;

    private final TransactionRepository transactionRepository;

    public Mono<Transaction> processPayment(SagaEvent event) {
        return balanceRepository.findByCustomerId(event.getOrderRequest().getOrder().getCustomerId())
                .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found")))
                .flatMap(customerBalance -> {

                    log.info("customerBalance :: {}", customerBalance);

                    if (customerBalance.getBalance() < event.getOrderRequest().getOrder().getTotalAmount()) {
                        return Mono.error(new InsufficientFundsException("Insufficient balance"));
                    }

                    customerBalance.setBalance(customerBalance.getBalance() - event.getOrderRequest().getOrder().getTotalAmount());
                    return balanceRepository.save(customerBalance);
                })
                .flatMap(savedBalance -> {
                    Transaction transaction = new Transaction();
                    Order order = event.getOrderRequest().getOrder();
                    transaction.setOrderId(order.getId());
                    transaction.setCustomerId(order.getCustomerId());
                    transaction.setAmount(order.getTotalAmount());
                    transaction.setMode(Helper.classifyPaymentMethod(order.getPaymentMethod()));
                    transaction.setStatus(PaymentStatus.COMPLETED);
                    transaction.setReferenceNumber(Helper.generateReferenceNumber());
                    transaction.setPaymentDate(LocalDateTime.now());

                    return transactionRepository.save(transaction);
                })
                .doOnSuccess(transaction -> {
                    SagaEvent sagaEvent = new SagaEvent(event.getSagaId(),
                            String.format("Payment processed for order %s", event.getOrderRequest().getOrder().getId()),
                            SagaEventType.PAYMENT_PROCESSED, event.getOrderRequest());
                    log.info("Sending PAYMENT_PROCESSED :: {}", sagaEvent);
                    sendPaymentProcessedEvent(sagaEvent);
                })
                .doOnError(error -> {
                    SagaEvent sagaEvent = new SagaEvent(event.getSagaId(),
                            String.format("Payment failed for order %s", event.getOrderRequest().getOrder().getId()),
                            SagaEventType.PAYMENT_FAILED, event.getOrderRequest());
                    log.error("Payment failed: ", error);
                    log.info("Sending PAYMENT_FAILED :: {}", sagaEvent);
                    sendPaymentFailedEvent(sagaEvent);
                });
    }

    private void sendPaymentProcessedEvent(SagaEvent event) {
        log.info("sendPaymentProcessedEvent");

        kafkaTemplate.send(sagaTopic,
                objectConverter.convertObjectToString(event));
    }

    private void sendPaymentFailedEvent(SagaEvent event) {
        log.info("sendPaymentFailedEvent");

        kafkaTemplate.send(sagaTopic,
                objectConverter.convertObjectToString(event));
    }
}
