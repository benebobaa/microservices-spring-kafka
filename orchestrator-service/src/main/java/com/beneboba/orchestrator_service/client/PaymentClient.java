package com.beneboba.orchestrator_service.client;

import com.beneboba.orchestrator_service.dto.payment.TransactionRequest;
import com.beneboba.orchestrator_service.dto.payment.TransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TransactionResponse.class);
    }
}
