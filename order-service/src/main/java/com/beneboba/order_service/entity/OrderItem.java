package com.beneboba.order_service.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@AllArgsConstructor
@Data
@Table("order_items")
public class OrderItem {

    @Id
    private Long id;

    private Long productId;

    private float price;

    @Min(1)
    private Integer quantity;

    private Long orderId;
}
