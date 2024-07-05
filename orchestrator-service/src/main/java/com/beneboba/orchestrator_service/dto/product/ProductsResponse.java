package com.beneboba.orchestrator_service.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProductsResponse {

    private List<ProductRequest> products;

    private float totalAmount;
}
