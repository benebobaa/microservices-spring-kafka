package com.beneboba.payment_service.repository;

import com.beneboba.payment_service.entity.Transaction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TransactionRepository extends ReactiveCrudRepository<Transaction, Long> {

    Mono<Transaction> findByOrderId(Long orderId);
}