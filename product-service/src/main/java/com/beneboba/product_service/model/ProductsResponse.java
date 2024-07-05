package com.beneboba.product_service.model;

import com.beneboba.product_service.entity.Product;
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
