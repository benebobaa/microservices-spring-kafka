package com.beneboba.order_service.entity;

import com.beneboba.order_service.model.OrderRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
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
@Table("orders")
public class Order {

    @Id
    private Long id;

    private Long customerId;

    private String billingAddress;

    private String shippingAddress;

    private OrderStatus orderStatus;

    private String paymentMethod;

    private float totalAmount;

    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    public OrderRequest toRequest(){
        return new OrderRequest(
                this.id,
                this.customerId,
                this.billingAddress,
                this.shippingAddress,
                this.orderStatus,
                this.totalAmount,
                this.paymentMethod,
                this.orderDate
        );
    }
}
