package com.beneboba.product_service.producer;

import com.beneboba.product_service.exception.ProductNotFoundException;
import com.beneboba.product_service.exception.StockNotEnoughException;
import com.beneboba.product_service.repository.ProductRepository;
import com.beneboba.product_service.util.ObjectConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.saga.OrderEvent;
import org.example.common.saga.SagaEvent;
import org.example.common.saga.SagaEventType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductProducer {
    @Value("${kafka.saga-topic.topics}")
    public String sagaTopic;

    private final ProductRepository productRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectConverter objectConverter;

    public Mono<Void> reserveProducts(String sagaId, OrderEvent orderEvent) {

        AtomicReference<Float> totalAmount = new AtomicReference<>(0.0f);

        return Flux.fromIterable(orderEvent.getProducts())
                .flatMap(productEvent -> {
                    Long productId = productEvent.getProductId();
                    int quantity = productEvent.getQuantity();

                    return productRepository.findById(productId)
                            .flatMap(product -> {
                                if (product.getStockQuantity() >= quantity) {
                                    product.setStockQuantity(product.getStockQuantity() - quantity);
                                    return productRepository.save(product)
                                            .doOnSuccess(savedProduct -> {
                                                log.info("Product reserved :: id {}", productId);
                                                productEvent.setPrice(product.getPrice());

                                                // Accumulate the total amount
                                                float productTotal = product.getPrice() * quantity;
                                                totalAmount.updateAndGet(amount -> amount + productTotal);
                                            });
                                } else {
                                    log.error("Not enough stock for product :: id {}", productId);
                                    return Mono.error(new StockNotEnoughException("Not enough stock for product: " + productId));
                                }
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.error("Product not found :: id {}", productId);
                                return Mono.error(new ProductNotFoundException("Product not found: " + productId));
                            }));
                })
                .then()
                .doOnSuccess(v -> {
                    orderEvent.getOrder().setTotalAmount(totalAmount.get());
                    SagaEvent event = new SagaEvent(sagaId, "Products reserved successfully",
                            SagaEventType.PRODUCT_RESERVED, orderEvent);

                    log.info("Sending message PRODUCT_RESERVED :: {}", event);
                    kafkaTemplate.send(sagaTopic,
                            objectConverter.convertObjectToString(event));
                })
                .doOnError(error -> {
                    log.error("Sending message PRODUCT_RESERVATION_FAILED :: {}", error.getMessage());
                    SagaEvent event = new SagaEvent(sagaId, error.getMessage(),
                            SagaEventType.PRODUCT_RESERVATION_FAILED, orderEvent);
                    kafkaTemplate.send(sagaTopic,
                            objectConverter.convertObjectToString(event));
                });
    }

    public Mono<Void> releaseProducts(String sagaId, OrderEvent orderEvent) {
        return Flux.fromIterable(orderEvent.getProducts())
                .flatMap(item -> productRepository.findById(item.getProductId())
                        .flatMap(product -> {
                            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                            return productRepository.save(product);
                        })
                )
                .then(Mono.fromRunnable(() -> {
                    SagaEvent event = new SagaEvent(sagaId, "Products released successfully",
                            SagaEventType.PRODUCT_RELEASED, orderEvent);
                    log.info("Sending PRODUCT_RELEASED event :: {}", event);
                    kafkaTemplate.send(sagaTopic, objectConverter.convertObjectToString(event));
                }))
                .then();
    }
}
