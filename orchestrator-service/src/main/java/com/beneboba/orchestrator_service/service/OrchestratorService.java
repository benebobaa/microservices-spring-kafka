package com.beneboba.orchestrator_service.service;

import com.beneboba.orchestrator_service.client.PaymentClient;
import com.beneboba.orchestrator_service.client.ProductClient;
import com.beneboba.orchestrator_service.dto.payment.PaymentStatus;
import com.beneboba.orchestrator_service.dto.payment.TransactionRequest;
import com.beneboba.orchestrator_service.dto.payment.TransactionResponse;
import com.beneboba.orchestrator_service.dto.product.ProductRequest;
import com.beneboba.orchestrator_service.dto.product.ProductsRequest;
import com.beneboba.orchestrator_service.dto.product.ProductsResponse;
import com.beneboba.orchestrator_service.util.ObjectConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.OrderStatus;
import org.example.common.SagaEvent;
import org.example.common.SagaEventType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorService {

    private final ProductClient productClient;

    private final PaymentClient paymentClient;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectConverter objectConverter;

    public Mono<Void> startProcessOrder(SagaEvent event) {
        log.info("startProcessOrder :: {}", event);

        List<ProductRequest> productRequests = event.getOrderRequest().getProducts().stream()
                .map(product -> new ProductRequest(product.getProductId(), product.getQuantity(), 0f))
                .collect(Collectors.toList());

        ProductsRequest request = new ProductsRequest(productRequests);
        log.info("Reserving products :: {}", request);

        return productClient.reserveProducts(request)
                .flatMap(productsResponse -> {
                    log.info("Products response :: {}", productsResponse);
                    event.getOrderRequest().getOrder()
                            .setTotalAmount(productsResponse.getTotalAmount());
                    productsResponse.getProducts().forEach(
                            product -> {
                                event.getOrderRequest().getProducts().forEach(item -> {
                                    if (product.getProductId().equals(item.getProductId())) {
                                        item.setPrice(product.getPrice());
                                    }
                                });
                            }
                    );

                    return processPayment(event, productsResponse);
                })
                .onErrorResume(e -> handleError(event, e, false))
                .then();
    }

    private Mono<Void> processPayment(SagaEvent event, ProductsResponse productsResponse) {
        TransactionRequest paymentRequest = createPaymentRequest(event, productsResponse);
        log.info("Processing payment :: {}", paymentRequest);

        return paymentClient.processPayment(paymentRequest)
                .flatMap(paymentResponse -> {
                    if (paymentResponse.getStatus().equals(PaymentStatus.COMPLETED)) {
                        log.info("Payment success response :: {}", paymentResponse);
                        return sendOrderStatusUpdate(
                                event,
                                SagaEventType.SAGA_COMPLETED
                        );
                    } else {
                        log.error("Payment failed response :: {}", paymentResponse);
                        return handleError(event, new RuntimeException("Payment failed"), true);
                    }
                })
                .onErrorResume(e -> handleError(event, e, true));
    }

    private Mono<Void> handleError(SagaEvent event, Throwable error, boolean needRollbackProduct) {
        log.error("Error processing order: {}", error.getMessage());
        log.info("Cancelling order :: {}", event);

        Mono<Void> errorHandling = sendOrderStatusUpdate(event, SagaEventType.SAGA_FAILED);

        if (needRollbackProduct) {
            errorHandling = errorHandling.then(releaseProducts(event));
        }

        return errorHandling;
    }

    private Mono<Void> releaseProducts(SagaEvent event) {
        List<ProductRequest> productRequests = event.getOrderRequest().getProducts().stream()
                .map(product -> new ProductRequest(product.getProductId(), product.getQuantity(), 0f))
                .collect(Collectors.toList());

        ProductsRequest request = new ProductsRequest(productRequests);
        return productClient.releasedProducts(request);
    }

    private Mono<Void> sendOrderStatusUpdate(SagaEvent event, SagaEventType type) {
        SagaEvent sagaEvent = new SagaEvent(
                event.getSagaId(),
                type.toString(),
                type,
                event.getOrderRequest()
        );

        return Mono.fromRunnable(() -> kafkaTemplate.send("order-topic",
                objectConverter.convertObjectToString(sagaEvent)));
    }

    private TransactionRequest createPaymentRequest(SagaEvent event, ProductsResponse productsResponse) {
        return new TransactionRequest(
                event.getOrderRequest().getOrder().getId(),
                event.getOrderRequest().getOrder().getCustomerId(),
                productsResponse.getTotalAmount(),
                event.getOrderRequest().getOrder().getPaymentMethod()
        );
    }
}
