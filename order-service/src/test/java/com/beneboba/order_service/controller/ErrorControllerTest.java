package com.beneboba.order_service.controller;

import com.beneboba.order_service.exception.ErrorResponse;
import com.beneboba.order_service.exception.OrderAlreadyCancelledException;
import com.beneboba.order_service.exception.OrderIncompleted;
import com.beneboba.order_service.exception.OrderNotFoundException;
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
    void handleOrderNotFoundException() {
        when(testController.throwOrderNotFoundException()).thenReturn(Mono.error(new OrderNotFoundException("Order not found")));

        webTestClient.get()
                .uri("/test/order-not-found")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assert errorResponse.getMessage().equals("Order not found");
                    assert errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST.value();
                });
    }

    @Test
    void handleOrderAlreadyCancelledException() {
        when(testController.throwOrderAlreadyCancelledException()).thenReturn(Mono.error(new OrderAlreadyCancelledException("Order already cancelled")));

        webTestClient.get()
                .uri("/test/order-already-cancelled")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assert errorResponse.getMessage().equals("Order already cancelled");
                    assert errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST.value();
                });
    }

    @Test
    void handleOrderIncompletedException() {
        when(testController.throwOrderIncompletedException()).thenReturn(Mono.error(new OrderIncompleted("Order not yet completed")));

        webTestClient.get()
                .uri("/test/order-incompleted")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assert errorResponse.getMessage().equals("Order not yet completed");
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

        @GetMapping("/test/order-not-found")
        public Mono<Void> throwOrderNotFoundException() {
            return Mono.error(new OrderNotFoundException("Order not found"));
        }

        @GetMapping("/test/order-already-cancelled")
        public Mono<Void> throwOrderAlreadyCancelledException() {
            return Mono.error(new OrderAlreadyCancelledException("Order already cancelled"));
        }

        @GetMapping("/test/order-incompleted")
        public Mono<Void> throwOrderIncompletedException() {
            return Mono.error(new OrderIncompleted("Order not yet completed"));
        }
    }
}