package com.beneboba.product_service.controller;

import com.beneboba.product_service.entity.Product;
import com.beneboba.product_service.model.ProductsResponse;
import com.beneboba.product_service.model.ProductsRequest;
import com.beneboba.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
class ProductController {
    
    private final ProductService productService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<Product> createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @PostMapping("/reserve")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ProductsResponse> reserveProducts(@RequestBody ProductsRequest productsRequest) {
        log.info("Reserve products :: {}", productsRequest);

        return productService.reserveProducts(productsRequest);
    }

    @PatchMapping("/release")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> releaseProducts(@RequestBody ProductsRequest productsRequest) {
        return productService.releaseProducts(productsRequest);
    }
}