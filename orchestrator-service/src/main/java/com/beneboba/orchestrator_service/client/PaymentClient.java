package com.beneboba.orchestrator_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.payment.TransactionRefundRequest;
import org.example.common.payment.TransactionRequest;
import org.example.common.payment.TransactionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class PaymentClient {

    private final WebClient webClient;

    public PaymentClient(
            WebClient.Builder webClientBuilder,
            @Value("${payment-api-baseurl}") String paymentApiBaseUrl
                         ) {
        log.info("PaymentClient :: paymentApiBaseUrl: {}", paymentApiBaseUrl);
        this.webClient = webClientBuilder.baseUrl(
                paymentApiBaseUrl
        ).build();
    }

    public Mono<TransactionResponse> processPayment(TransactionRequest request) {
        return webClient.post()
                .uri("/api/payments/create")
                .body(Mono.just(request), TransactionRequest.class)
                .retrieve()
                .bodyToMono(TransactionResponse.class)
                .doOnNext(transactionResponse -> log.info("Payment processed :: {}", transactionResponse))
                .doOnError(throwable -> log.error("Payment failed :: {}", throwable.getMessage()));
    }

    public Mono<TransactionResponse> refundPayment(TransactionRefundRequest request) {
        return webClient.patch()
                .uri("/api/payments/refund")
                .body(Mono.just(request), TransactionRefundRequest.class)
                .retrieve()
                .bodyToMono(TransactionResponse.class)
                .doOnNext(transactionResponse -> log.info("Payment refunded :: {}", transactionResponse))
                .doOnError(throwable -> log.error("Payment refund failed :: {}", throwable.getMessage()));
    }
}
