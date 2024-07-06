package com.beneboba.payment_service.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

@Data
@Table("customer_balance")
public class Balance {

    @Id
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    private Long customerId;

    private float balance;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Balance balance1 = (Balance) o;
        return Float.compare(balance, balance1.balance) == 0 && Objects.equals(id, balance1.id) && Objects.equals(customerId, balance1.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerId, balance);
    }
}