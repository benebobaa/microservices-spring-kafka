package com.beneboba.product_service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ProductsRequest {

    @Valid
    @NotNull
    private List<ProductRequest> products;

}
