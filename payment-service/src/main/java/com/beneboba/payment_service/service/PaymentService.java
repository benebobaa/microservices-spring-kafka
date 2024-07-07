package com.beneboba.payment_service.service;

import com.beneboba.payment_service.exception.CustomerIdNotMatchException;
import com.beneboba.payment_service.exception.CustomerNotFoundException;
import com.beneboba.payment_service.exception.InsufficientFundsException;
import com.beneboba.payment_service.entity.Balance;
import com.beneboba.payment_service.entity.PaymentStatus;
import com.beneboba.payment_service.entity.Transaction;
import com.beneboba.payment_service.exception.TransactionNotFoundException;
import com.beneboba.payment_service.repository.BalanceRepository;
import com.beneboba.payment_service.repository.TransactionRepository;
import com.beneboba.payment_service.util.Helper;
import com.beneboba.payment_service.util.ObjectConverter;
import com.beneboba.payment_service.util.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.saga.Order;
import org.example.common.saga.SagaEvent;
import org.example.common.saga.SagaEventType;
import org.example.common.payment.TransactionRefundRequest;
import org.example.common.payment.TransactionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;

    private final BalanceRepository balanceRepository;

    private final ValidationService validationService;


    public Flux<Balance> getAllCustomerBalance() {
        return balanceRepository.findAll();
    }

    public Flux<Transaction> getAllTransaction() {
        return transactionRepository.findAll();
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
//                    Transaction transaction = request.toEntity();
                    Transaction transaction = new Transaction(
                            null, request.getOrderId(), request.getCustomerId(), request.getAmount(),
                            Helper.classifyPaymentMethod(request.getPaymentMethod()),
                            PaymentStatus.COMPLETED, Helper.generateReferenceNumber(), null
                    );
                    transaction.setPaymentDate(LocalDateTime.now());
                    return transactionRepository.save(transaction);
                })
                .doOnSuccess(transaction -> log.info("Create transaction completed: {}", transaction))
                .doOnError(throwable -> log.error("Error occurred while creating transaction: {}", throwable.getMessage()));
    }

    public Mono<Transaction> refundTransaction(TransactionRefundRequest request) {
        return validationService.validate(request)
                .flatMap(validRequest -> transactionRepository.findByOrderId(validRequest.getOrderId())
                        .switchIfEmpty(Mono.error(new TransactionNotFoundException("Transaction not found"))))
                .flatMap(transaction -> balanceRepository.findByCustomerId(request.getCustomerId())
                        .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found")))
                        .flatMap(balance -> {
                            if (!Objects.equals(transaction.getCustomerId(), balance.getCustomerId())){
                                log.error("Customer id not match :: transaction.customerId: {}, balance.customerId: {}",
                                        transaction.getCustomerId(), balance.getCustomerId());
                                return Mono.error(new CustomerIdNotMatchException("Customer id not match"));
                            }

                            balance.setBalance(balance.getBalance() + (transaction.getAmount()));
                            return balanceRepository.save(balance)
                                    .thenReturn(transaction);
                        }))
                .flatMap(transaction -> {
                    transaction.setPaymentDate(LocalDateTime.now());
                    transaction.setStatus(PaymentStatus.REFUNDED);
                    return transactionRepository.save(transaction);
                })
                .doOnSuccess(transaction -> log.info("Refund transaction completed: {}", transaction))
                .doOnError(throwable -> log.error("Error occurred while refunding transaction: {}", throwable.getMessage()));
    }
}