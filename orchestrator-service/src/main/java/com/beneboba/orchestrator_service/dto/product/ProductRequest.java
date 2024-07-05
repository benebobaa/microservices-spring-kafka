package com.beneboba.orchestrator_service.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ProductRequest {

    private Long productId;

    private Integer quantity;

    private float price;
}
