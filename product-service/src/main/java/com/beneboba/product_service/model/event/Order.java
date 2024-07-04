package com.beneboba.product_service.model.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private Long id;

    private Long customerId;

    private String billingAddress;

    private String shippingAddress;

    private OrderStatus orderStatus;

    private String paymentMethod;

    private float totalAmount;

    private LocalDate orderDate;
}
