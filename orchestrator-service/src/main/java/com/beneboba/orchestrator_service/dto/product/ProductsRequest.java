package com.beneboba.orchestrator_service.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class ProductsRequest {

    private List<ProductRequest> products;
}
