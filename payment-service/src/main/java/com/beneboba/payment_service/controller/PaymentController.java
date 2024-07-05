package com.beneboba.payment_service.controller;

import com.beneboba.payment_service.entity.Balance;
import com.beneboba.payment_service.entity.Transaction;
import com.beneboba.payment_service.model.TransactionRequest;
import com.beneboba.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<Transaction> getAllTransaction() {
        return paymentService.getAllTransaction();
    }

    @GetMapping("/balance")
    @ResponseStatus(HttpStatus.OK)
    public Flux<Balance> getAllCustomerBalance() {
        return paymentService.getAllCustomerBalance();
    }

    @PostMapping("/balance")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Balance> addBalance(
            @RequestBody Balance balance
            ) {
        return paymentService.createNewBalance(balance);
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Transaction> createTransaction(
            @RequestBody TransactionRequest request
    ) {
        log.info("Create transaction :: {}", request);
        return paymentService.createNewTransaction(request);
    }
}