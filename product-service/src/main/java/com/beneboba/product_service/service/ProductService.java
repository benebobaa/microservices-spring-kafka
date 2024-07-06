package com.beneboba.product_service.service;

import com.beneboba.product_service.exception.ProductNotFoundException;
import com.beneboba.product_service.exception.StockNotEnoughException;
import com.beneboba.product_service.entity.Product;
import com.beneboba.product_service.model.ProductRequest;
import com.beneboba.product_service.model.ProductsResponse;
import com.beneboba.product_service.model.ProductsRequest;
import com.beneboba.product_service.repository.ProductRepository;
import com.beneboba.product_service.util.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    private final ValidationService validationService;

    public Flux<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Mono<Product> createProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public Mono<ProductsResponse> reserveProducts(ProductsRequest request) {
        return validationService.validate(request)
                .flatMap(validRequest -> Flux.fromIterable(validRequest.getProducts())
                        .flatMap(this::validateAndReserveProduct)
                        .collectList()
                )
                .map(this::createProductsResponse)
                .onErrorMap(throwable -> {
                    if (throwable instanceof ProductNotFoundException ||
                            throwable instanceof StockNotEnoughException) {
                        return throwable;
                    }
                    return new RuntimeException("An unexpected error occurred", throwable);
                });
    }

    @Transactional
    public Mono<Void> releaseProducts(ProductsRequest request) {
        return Flux.fromIterable(request.getProducts())
                .flatMap(this::releaseProduct)
                .then()
                .doOnSuccess(
                        success -> log.info("Products released successfully: {}", request)
                );
    }

    private Mono<Void> releaseProduct(ProductRequest productRequest) {
        return productRepository.findById(productRequest.getProductId())
                .switchIfEmpty(Mono.error(new ProductNotFoundException("Product not found: " + productRequest.getProductId())))
                .flatMap(product -> {
                    int newStockQuantity = product.getStockQuantity() + productRequest.getQuantity();
                    product.setStockQuantity(newStockQuantity);
                    return productRepository.save(product);
                })
                .then();
    }

    private Mono<ProductRequest> validateAndReserveProduct(ProductRequest productRequest) {
        return productRepository.findById(productRequest.getProductId())
                .switchIfEmpty(Mono.error(new ProductNotFoundException("Product not found: " + productRequest.getProductId())))
                .flatMap(product -> {
                    if (product.getStockQuantity() < productRequest.getQuantity()) {
                        return Mono.error(new StockNotEnoughException("Not enough stock for product: " + product.getId()));
                    }
                    int newStockQuantity = product.getStockQuantity() - productRequest.getQuantity();
                    product.setStockQuantity(newStockQuantity);
                    return productRepository.save(product)
                            .then(Mono.just(productRequest.setPrice(product.getPrice())));
                });
    }

    private ProductsResponse createProductsResponse(List<ProductRequest> reservedProducts) {
        float totalAmount = reservedProducts.stream()
                .map(product -> product.getPrice() * product.getQuantity())
                .reduce(0f, Float::sum);
        return new ProductsResponse(reservedProducts, totalAmount);
    }
}