package com.beneboba.payment_service.controller;

import com.beneboba.payment_service.model.Balance;
import com.beneboba.payment_service.model.Transaction;
import com.beneboba.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
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
}