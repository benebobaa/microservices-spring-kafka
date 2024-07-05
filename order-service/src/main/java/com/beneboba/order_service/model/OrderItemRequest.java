package com.beneboba.order_service.model;

import com.beneboba.order_service.entity.OrderItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;

@AllArgsConstructor
@Data
public class OrderItemRequest {

    @Id
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    private Long productId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private float price;

    @Min(1)
    private Integer quantity;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long orderId;

    public OrderItem toEntity(){
        return new OrderItem(
                this.id,
                this.productId,
                this.price,
                this.quantity,
                this.orderId
        );
    }

    public org.example.common.OrderItem toEvent(){
        return new org.example.common.OrderItem(
                this.id,
                this.productId,
                this.price,
                this.quantity,
                this.orderId
        );
    }
}