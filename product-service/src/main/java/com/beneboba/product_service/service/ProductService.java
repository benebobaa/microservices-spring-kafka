package com.beneboba.product_service.service;

import com.beneboba.product_service.exception.ProductNotFoundException;
import com.beneboba.product_service.exception.StockNotEnoughException;
import com.beneboba.product_service.model.Product;
import com.beneboba.product_service.model.event.*;
import com.beneboba.product_service.repository.ProductRepository;
import com.beneboba.product_service.util.ObjectConverter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectConverter objectConverter;

    public Flux<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Mono<Product> createProduct(Product product) {
        return productRepository.save(product);
    }

    public Mono<Void> reserveProducts(String sagaId,OrderEvent orderEvent) {

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
                            }))
                            .doOnError(error -> {
                                log.error("Product reservation failed :: id {}", productId);
                                SagaEvent event = new SagaEvent(sagaId, error.getMessage(),
                                         SagaEventType.PRODUCT_RESERVATION_FAILED, orderEvent);
                                kafkaTemplate.send("saga-topic",
                                        objectConverter.convertObjectToString(event));
                            });
                })
                .then()
                .doOnSuccess(v -> {
                    orderEvent.getOrder().setTotalAmount(totalAmount.get());
                    SagaEvent event = new SagaEvent(sagaId, "Products reserved successfully",
                            SagaEventType.PRODUCT_RESERVED, orderEvent);

                    log.info("Sending message PRODUCT_RESERVED :: {}", event);
                    kafkaTemplate.send("saga-topic",
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
                    kafkaTemplate.send("saga-topic", objectConverter.convertObjectToString(event));
                }))
                .then();
    }
}