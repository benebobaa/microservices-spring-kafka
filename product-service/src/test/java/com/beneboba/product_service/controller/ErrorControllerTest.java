package com.beneboba.product_service.controller;

import com.beneboba.product_service.exception.ErrorResponse;
import com.beneboba.product_service.exception.ProductNotFoundException;
import com.beneboba.product_service.exception.StockNotEnoughException;
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
    void handleProductNotFoundException() {
        when(testController.throwProductNotFoundException()).thenReturn(Mono.error(new ProductNotFoundException("Product not found")));

        webTestClient.get()
                .uri("/test/product-not-found")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assert errorResponse.getMessage().equals("Product not found");
                    assert errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST.value();
                });
    }

    @Test
    void handleStockNotEnoughException() {
        when(testController.throwStockNotEnoughException()).thenReturn(Mono.error(new StockNotEnoughException("Stock not enough")));

        webTestClient.get()
                .uri("/test/stock-not-enough")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assert errorResponse.getMessage().equals("Stock not enough");
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

        @GetMapping("/test/product-not-found")
        public Mono<Void> throwProductNotFoundException() {
            return Mono.error(new ProductNotFoundException("Product not found"));
        }

        @GetMapping("/test/stock-not-enough")
        public Mono<Void> throwStockNotEnoughException() {
            return Mono.error(new StockNotEnoughException("Stock not enough"));
        }
    }
}