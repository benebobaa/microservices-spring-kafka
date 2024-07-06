package com.beneboba.orchestrator_service.client;

import lombok.extern.slf4j.Slf4j;
import org.example.common.product.ProductsRequest;
import org.example.common.product.ProductsResponse;
import org.example.common.saga.OrderEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ProductClient {


    private final WebClient webClient;

    public ProductClient(WebClient.Builder webClientBuilder,
                         @Value("${product-api-baseurl}") String productApiBaseUrl
    ) {
        log.info("ProductClient :: productApiBaseUrl: {}", productApiBaseUrl);
        this.webClient = webClientBuilder.baseUrl(
                productApiBaseUrl
        ).build();
    }

    public Mono<ProductsResponse> reserveProducts(ProductsRequest request) {
        return webClient.post()
                .uri("/api/products/reserve")
                .body(Mono.just(request), ProductsRequest.class)
                .retrieve()
                .bodyToMono(ProductsResponse.class)
                .doOnNext(productsResponse -> log.info("Products reserved :: {}", productsResponse))
                .doOnError(throwable -> log.error("Product reservation failed :: {}", throwable.getMessage()));
    }

    public Mono<Void> releasedProducts(ProductsRequest request) {
        return webClient.patch()
                .uri("/api/products/release")
                .body(Mono.just(request), ProductsRequest.class)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnNext(productsResponse -> log.info("Products released :: {}", productsResponse))
                .doOnError(throwable -> log.error("Product release failed :: {}", throwable.getMessage()));
    }
}
