package com.beneboba.payment_service.repository;

import com.beneboba.payment_service.entity.Balance;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface BalanceRepository extends ReactiveCrudRepository<Balance, Long> {
    Mono<Balance> findByCustomerId(Long customerId);
}