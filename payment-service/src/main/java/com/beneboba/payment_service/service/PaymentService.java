package com.beneboba.payment_service.service;

import com.beneboba.payment_service.exception.CustomerNotFoundException;
import com.beneboba.payment_service.exception.InsufficientFundsException;
import com.beneboba.payment_service.entity.Balance;
import com.beneboba.payment_service.entity.PaymentStatus;
import com.beneboba.payment_service.entity.Transaction;
import com.beneboba.payment_service.model.TransactionRequest;
import com.beneboba.payment_service.repository.BalanceRepository;
import com.beneboba.payment_service.repository.TransactionRepository;
import com.beneboba.payment_service.util.Helper;
import com.beneboba.payment_service.util.ObjectConverter;
import com.beneboba.payment_service.util.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.Order;
import org.example.common.SagaEvent;
import org.example.common.SagaEventType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${kafka.saga-topic.topics}")
    private String sagaTopic;

    private final TransactionRepository transactionRepository;

    private final BalanceRepository balanceRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectConverter objectConverter;

    private final ValidationService validationService;

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

    public Flux<Balance> getAllCustomerBalance() {
        return balanceRepository.findAll();
    }

    public Flux<Transaction> getAllTransaction() {
        return transactionRepository.findAll();
    }

    public Mono<Balance> createNewBalance(Balance balance) {
        return balanceRepository.save(balance);
    }

    public Mono<Transaction> createNewTransaction(TransactionRequest request) {
        return validationService.validate(request)
                .flatMap(validRequest -> balanceRepository.findByCustomerId(validRequest.getCustomerId()))
                .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found")))
                .flatMap(
                        customerBalance -> {
                            if (customerBalance.getBalance() < request.getAmount()) {
                                return Mono.error(new InsufficientFundsException("Insufficient balance"));
                            }
                            customerBalance.setBalance(customerBalance.getBalance() - request.getAmount());
                            return balanceRepository.save(customerBalance);
                        }
                )
                .flatMap(savedBalance -> {
                    Transaction transaction = request.toEntity();
                    transaction.setPaymentDate(LocalDateTime.now());
                    return transactionRepository.save(transaction);
                });
    }
}