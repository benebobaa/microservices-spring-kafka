package com.beneboba.orchestrator_service.service;

import com.beneboba.orchestrator_service.client.PaymentClient;
import com.beneboba.orchestrator_service.client.ProductClient;
import com.beneboba.orchestrator_service.util.ObjectConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.payment.PaymentStatus;
import org.example.common.payment.TransactionRefundRequest;
import org.example.common.payment.TransactionRequest;
import org.example.common.product.ProductRequest;
import org.example.common.product.ProductsRequest;
import org.example.common.product.ProductsResponse;
import org.example.common.saga.SagaEvent;
import org.example.common.saga.SagaEventType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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

    public Mono<Void> processEvent(SagaEvent event) {
        log.info("Processing event :: {}", event);

        return switch (event.getType()) {
            case ORDER_CREATED -> startProcessOrder(event);
            case ORDER_CANCELLED -> cancelOrder(event);
            default -> {
                log.error("Unsupported event type: {}", event.getType());
                yield Mono.error(new UnsupportedOperationException("Unsupported event type"));
            }
        };
    }

    private Mono<Void> startProcessOrder(SagaEvent event) {
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
                    updateProductPrices(event, productsResponse);

                    return processPayment(event, productsResponse);
                })
                .onErrorResume(e -> handleError(event, e, false))
                .then();
    }

    private Mono<Void> cancelOrder(SagaEvent event) {
        log.info("cancelOrder :: {}", event);

        return releaseProducts(event)
                .then(refundPayment(event))
                .then(sendOrderStatusUpdate(event, SagaEventType.SAGA_COMPLETED))
                .onErrorResume(e -> handleError(event, e, false));
    }

    private Mono<Void> processPayment(SagaEvent event, ProductsResponse productsResponse) {
        TransactionRequest paymentRequest = createPaymentRequest(event, productsResponse);
        log.info("Processing payment :: {}", paymentRequest);

        return paymentClient.processPayment(paymentRequest)
                .flatMap(paymentResponse -> {
                    if (paymentResponse.getStatus().equals(PaymentStatus.COMPLETED)) {
                        log.info("Payment success response :: {}", paymentResponse);
                        return sendOrderStatusUpdate(event, SagaEventType.SAGA_COMPLETED);
                    } else {
                        log.error("Payment failed response :: {}", paymentResponse);
                        return handleError(event, new RuntimeException("Payment failed"), true);
                    }
                })
                .onErrorResume(e -> handleError(event, e, true));
    }

    private Mono<Void> refundPayment(SagaEvent event) {
        TransactionRefundRequest refundRequest = createRefundRequest(event);
        log.info("Refunding payment :: {}", refundRequest);

        return paymentClient.refundPayment(refundRequest)
                .flatMap(refundResponse -> {
                    if (refundResponse.getStatus().equals(PaymentStatus.REFUNDED)) {
                        log.info("Refund success response :: {}", refundResponse);
                        return Mono.empty();
                    } else {
                        log.error("Refund failed response :: {}", refundResponse);
                        return Mono.error(new RuntimeException("Refund failed"));
                    }
                });
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

    private TransactionRefundRequest createRefundRequest(SagaEvent event) {
        return new TransactionRefundRequest(
                event.getOrderRequest().getOrder().getId(),
                event.getOrderRequest().getOrder().getCustomerId()
        );
    }

    private void updateProductPrices(SagaEvent event, ProductsResponse productsResponse) {
        productsResponse.getProducts().forEach(
                product -> event.getOrderRequest().getProducts().forEach(item -> {
                    if (product.getProductId().equals(item.getProductId())) {
                        item.setPrice(product.getPrice());
                    }
                })
        );
    }
}