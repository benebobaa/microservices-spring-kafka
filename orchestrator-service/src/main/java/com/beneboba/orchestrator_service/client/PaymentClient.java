package com.beneboba.orchestrator_service.client;

import com.beneboba.orchestrator_service.dto.payment.TransactionRequest;
import com.beneboba.orchestrator_service.dto.payment.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class PaymentClient {

    @Value("${product-api-baseurl}")
    private String paymentApiBaseUrl;

    private final WebClient webClient;

    public PaymentClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(
                "http://localhost:8083"
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
