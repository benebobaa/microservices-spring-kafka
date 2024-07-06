package com.beneboba.product_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProductRequest {

    @NotNull
    private Long productId;

    @Min(1)
    private Integer quantity;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private float price;

    public ProductRequest setPrice(float price) {
        this.price = price;
        return this;
    }
}
