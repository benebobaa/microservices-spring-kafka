package com.beneboba.payment_service.controller;

import com.beneboba.payment_service.controller.ErrorController;
import com.beneboba.payment_service.exception.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {ErrorController.class, ErrorControllerTest.TestController.class})
class ErrorControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TestController testController;

    @BeforeEach
    void setUp() {
    }

    @Test
    void handleException() {
        when(testController.throwException()).thenReturn(Mono.error(new Exception("Test exception")));

        webTestClient.get()
                .uri("/test/exception")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assert errorResponse.getMessage().equals("Test exception");
                    assert errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST.value();
                });
    }

    @Test
    void handleConstraintViolationException() {
        when(testController.throwConstraintViolationException()).thenReturn(Mono.error(new ConstraintViolationException("Constraint violation", null)));

        webTestClient.get()
                .uri("/test/constraint-violation")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assert errorResponse.getMessage().equals("Constraint violation");
                    assert errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST.value();
                });
    }

    @Test
    void handleInsufficientFundsException() {
        when(testController.throwInsufficientFundsException()).thenReturn(Mono.error(new InsufficientFundsException("Insufficient funds")));

        webTestClient.get()
                .uri("/test/insufficient-funds")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assert errorResponse.getMessage().equals("Insufficient funds");
                    assert errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST.value();
                });
    }

    @Test
    void handleCustomerNotFoundException() {
        when(testController.throwCustomerNotFoundException()).thenReturn(Mono.error(new CustomerNotFoundException("Customer not found")));

        webTestClient.get()
                .uri("/test/customer-not-found")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assert errorResponse.getMessage().equals("Customer not found");
                    assert errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST.value();
                });
    }

    @Test
    void handleTransactionNotFoundException() {
        when(testController.throwTransactionNotFoundException()).thenReturn(Mono.error(new TransactionNotFoundException("Transaction not found")));

        webTestClient.get()
                .uri("/test/transaction-not-found")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assert errorResponse.getMessage().equals("Transaction not found");
                    assert errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST.value();
                });
    }

    @Test
    void handleCustomerIdNotMatchException() {
        when(testController.throwCustomerIdNotMatch()).thenReturn(Mono.error(new CustomerIdNotMatchException("Customer id not match")));

        webTestClient.get()
                .uri("/test/customerid-not-match")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assert errorResponse.getMessage().equals("Customer id not match");
                    assert errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST.value();
                });
    }

    @RestController
    static class TestController {
        @GetMapping("/test/exception")
        public Mono<Void> throwException() {
            return Mono.error(new Exception("Test exception"));
        }

        @GetMapping("/test/constraint-violation")
        public Mono<Void> throwConstraintViolationException() {
            return Mono.error(new ConstraintViolationException("Constraint violation", null));
        }

        @GetMapping("/test/insufficient-funds")
        public Mono<Void> throwInsufficientFundsException() {
            return Mono.error(new InsufficientFundsException("Insufficient funds"));
        }

        @GetMapping("/test/customer-not-found")
        public Mono<Void> throwCustomerNotFoundException() {
            return Mono.error(new CustomerNotFoundException("Customer not found"));
        }

        @GetMapping("/test/transaction-not-found")
        public Mono<Void> throwTransactionNotFoundException() {
            return Mono.error(new TransactionNotFoundException("Transaction not found"));
        }

        @GetMapping("/test/customerid-not-match")
        public Mono<Void> throwCustomerIdNotMatch() {
            return Mono.error(new CustomerIdNotMatchException("Customer id not match"));
        }
    }
}