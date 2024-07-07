package com.beneboba.product_service.service;

import com.beneboba.product_service.entity.Product;
import com.beneboba.product_service.exception.ProductNotFoundException;
import com.beneboba.product_service.exception.StockNotEnoughException;
import com.beneboba.product_service.model.ProductRequest;
import com.beneboba.product_service.model.ProductsRequest;
import com.beneboba.product_service.repository.ProductRepository;
import com.beneboba.product_service.util.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAllProducts() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Product 1");
        when(productRepository.findAll()).thenReturn(Flux.just(product));

        StepVerifier.create(productService.getAllProducts())
                .expectNextMatches(p -> p.getName().equals("Product 1"))
                .verifyComplete();
    }

    @Test
    public void testCreateProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Product 1");
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product));

        StepVerifier.create(productService.createProduct(product))
                .expectNextMatches(p -> p.getName().equals("Product 1"))
                .verifyComplete();
    }

    @Test
    public void testReserveProducts() {
        ProductRequest productRequest = new ProductRequest();
        productRequest.setProductId(1L);
        productRequest.setQuantity(1);

        ProductsRequest productsRequest = new ProductsRequest();
        productsRequest.setProducts(List.of(productRequest));

        Product product = new Product();
        product.setId(1L);
        product.setStockQuantity(10);
        product.setPrice(100f);

        when(validationService.validate(productsRequest)).thenReturn(Mono.just(productsRequest));
        when(productRepository.findById(1L)).thenReturn(Mono.just(product));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product));

        StepVerifier.create(productService.reserveProducts(productsRequest))
                .expectNextMatches(response -> response.getTotalAmount() == 100f)
                .verifyComplete();
    }

    @Test
    public void testReleaseProducts() {
        ProductRequest productRequest = new ProductRequest();
        productRequest.setProductId(1L);
        productRequest.setQuantity(1);

        ProductsRequest productsRequest = new ProductsRequest();
        productsRequest.setProducts(List.of(productRequest));

        Product product = new Product();
        product.setId(1L);
        product.setStockQuantity(10);

        when(productRepository.findById(1L)).thenReturn(Mono.just(product));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product));

        StepVerifier.create(productService.releaseProducts(productsRequest))
                .verifyComplete();
    }

    @Test
    public void testReserveProductsNotFound() {
        ProductRequest productRequest = new ProductRequest();
        productRequest.setProductId(1L);
        productRequest.setQuantity(1);

        ProductsRequest productsRequest = new ProductsRequest();
        productsRequest.setProducts(List.of(productRequest));

        when(validationService.validate(productsRequest)).thenReturn(Mono.just(productsRequest));
        when(productRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(productService.reserveProducts(productsRequest))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    public void testReserveProductsNotEnoughStock() {
        ProductRequest productRequest = new ProductRequest();
        productRequest.setProductId(1L);
        productRequest.setQuantity(10);

        ProductsRequest productsRequest = new ProductsRequest();
        productsRequest.setProducts(List.of(productRequest));

        Product product = new Product();
        product.setId(1L);
        product.setStockQuantity(5);

        when(validationService.validate(productsRequest)).thenReturn(Mono.just(productsRequest));
        when(productRepository.findById(1L)).thenReturn(Mono.just(product));

        StepVerifier.create(productService.reserveProducts(productsRequest))
                .expectError(StockNotEnoughException.class)
                .verify();
    }
}
