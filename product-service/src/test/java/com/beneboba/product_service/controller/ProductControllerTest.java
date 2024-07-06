package com.beneboba.product_service.controller;

import com.beneboba.product_service.entity.Product;
import com.beneboba.product_service.model.ProductRequest;
import com.beneboba.product_service.model.ProductsRequest;
import com.beneboba.product_service.model.ProductsResponse;
import com.beneboba.product_service.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProductService productService;

    private Product product;
    private ProductRequest productRequest;
    private ProductsRequest productsRequest;

    @BeforeEach
    public void setup() {
        product = new Product();
        product.setId(1L);
        product.setName("Product 1");
        product.setPrice(100f);
        product.setStockQuantity(10);

        productRequest = new ProductRequest();
        productRequest.setProductId(1L);
        productRequest.setQuantity(1);

        productsRequest = new ProductsRequest();
        productsRequest.setProducts(List.of(productRequest));
    }

    @Test
    public void testGetAllProducts() {
        when(productService.getAllProducts()).thenReturn(Flux.just(product));

        webTestClient.get().uri("/api/products")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testCreateProduct() {
        when(productService.createProduct(any(Product.class))).thenReturn(Mono.just(product));

        webTestClient.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(product)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testReserveProducts() {
        ProductsResponse productsResponse = new ProductsResponse(List.of(productRequest.setPrice(100f)), 100f);
        when(productService.reserveProducts(any(ProductsRequest.class))).thenReturn(Mono.just(productsResponse));

        webTestClient.post().uri("/api/products/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productsRequest)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testReleaseProducts() {
        when(productService.releaseProducts(any(ProductsRequest.class))).thenReturn(Mono.empty());

        webTestClient.patch().uri("/api/products/release")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productsRequest)
                .exchange()
                .expectStatus().isOk();
    }
}
