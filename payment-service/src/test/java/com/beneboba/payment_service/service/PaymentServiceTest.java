package com.beneboba.payment_service.service;

import com.beneboba.payment_service.entity.Balance;
import com.beneboba.payment_service.entity.PaymentStatus;
import com.beneboba.payment_service.entity.Transaction;
import com.beneboba.payment_service.exception.CustomerNotFoundException;
import com.beneboba.payment_service.exception.InsufficientFundsException;
import com.beneboba.payment_service.exception.TransactionNotFoundException;
import com.beneboba.payment_service.model.TransactionRefundRequest;
import com.beneboba.payment_service.model.TransactionRequest;
import com.beneboba.payment_service.repository.BalanceRepository;
import com.beneboba.payment_service.repository.TransactionRepository;
import com.beneboba.payment_service.util.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private PaymentService paymentService;

    private TransactionRequest validTransactionRequest;
    private TransactionRefundRequest validTransactionRefundRequest;
    private Balance customerBalance;
    private Transaction transaction;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        validTransactionRequest = new TransactionRequest();
        validTransactionRequest.setOrderId(1L);
        validTransactionRequest.setCustomerId(1L);
        validTransactionRequest.setAmount(100f);
        validTransactionRequest.setPaymentMethod("BCA");

        validTransactionRefundRequest = new TransactionRefundRequest();
        validTransactionRefundRequest.setOrderId(1L);
        validTransactionRefundRequest.setCustomerId(1L);

        customerBalance = new Balance();
        customerBalance.setId(1L);
        customerBalance.setCustomerId(1L);
        customerBalance.setBalance(500f);

        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setOrderId(1L);
        transaction.setAmount(100f);
        transaction.setMode("BCA");
        transaction.setStatus(PaymentStatus.COMPLETED);
        transaction.setReferenceNumber("12345678");
        transaction.setPaymentDate(LocalDateTime.now());
    }

    @Test
    public void testGetAllCustomerBalance() {
        when(balanceRepository.findAll()).thenReturn(Flux.just(customerBalance));

        StepVerifier.create(paymentService.getAllCustomerBalance())
                .expectNext(customerBalance)
                .verifyComplete();

        verify(balanceRepository, times(1)).findAll();
    }

    @Test
    public void testGetAllTransaction() {
        when(transactionRepository.findAll()).thenReturn(Flux.just(transaction));

        StepVerifier.create(paymentService.getAllTransaction())
                .expectNext(transaction)
                .verifyComplete();

        verify(transactionRepository, times(1)).findAll();
    }

    @Test
    public void testCreateNewTransaction() {
        when(validationService.validate(any(TransactionRequest.class))).thenReturn(Mono.just(validTransactionRequest));
        when(balanceRepository.findByCustomerId(validTransactionRequest.getCustomerId())).thenReturn(Mono.just(customerBalance));
        when(balanceRepository.save(any(Balance.class))).thenReturn(Mono.just(customerBalance));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(Mono.just(transaction));

        StepVerifier.create(paymentService.createNewTransaction(validTransactionRequest))
                .expectNext(transaction)
                .verifyComplete();

        verify(validationService, times(1)).validate(validTransactionRequest);
        verify(balanceRepository, times(1)).findByCustomerId(validTransactionRequest.getCustomerId());
        verify(balanceRepository, times(1)).save(customerBalance);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    public void testCreateNewTransactionCustomerNotFound() {
        when(validationService.validate(any(TransactionRequest.class))).thenReturn(Mono.just(validTransactionRequest));
        when(balanceRepository.findByCustomerId(validTransactionRequest.getCustomerId())).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.createNewTransaction(validTransactionRequest))
                .expectError(CustomerNotFoundException.class)
                .verify();

        verify(validationService, times(1)).validate(validTransactionRequest);
        verify(balanceRepository, times(1)).findByCustomerId(validTransactionRequest.getCustomerId());
        verify(balanceRepository, never()).save(any(Balance.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    public void testCreateNewTransactionInsufficientFunds() {
        customerBalance.setBalance(50f);
        when(validationService.validate(any(TransactionRequest.class))).thenReturn(Mono.just(validTransactionRequest));
        when(balanceRepository.findByCustomerId(validTransactionRequest.getCustomerId())).thenReturn(Mono.just(customerBalance));

        StepVerifier.create(paymentService.createNewTransaction(validTransactionRequest))
                .expectError(InsufficientFundsException.class)
                .verify();

        verify(validationService, times(1)).validate(validTransactionRequest);
        verify(balanceRepository, times(1)).findByCustomerId(validTransactionRequest.getCustomerId());
        verify(balanceRepository, never()).save(any(Balance.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    public void testRefundTransaction() {
        when(validationService.validate(any(TransactionRefundRequest.class))).thenReturn(Mono.just(validTransactionRefundRequest));
        when(transactionRepository.findByOrderId(validTransactionRefundRequest.getOrderId())).thenReturn(Mono.just(transaction));
        when(balanceRepository.findByCustomerId(validTransactionRefundRequest.getCustomerId())).thenReturn(Mono.just(customerBalance));
        when(balanceRepository.save(any(Balance.class))).thenReturn(Mono.just(customerBalance));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(Mono.just(transaction));

        StepVerifier.create(paymentService.refundTransaction(validTransactionRefundRequest))
                .expectNext(transaction)
                .verifyComplete();

        verify(validationService, times(1)).validate(validTransactionRefundRequest);
        verify(transactionRepository, times(1)).findByOrderId(validTransactionRefundRequest.getOrderId());
        verify(balanceRepository, times(1)).findByCustomerId(validTransactionRefundRequest.getCustomerId());
        verify(balanceRepository, times(1)).save(customerBalance);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    public void testRefundTransactionTransactionNotFound() {
        when(validationService.validate(any(TransactionRefundRequest.class))).thenReturn(Mono.just(validTransactionRefundRequest));
        when(transactionRepository.findByOrderId(validTransactionRefundRequest.getOrderId())).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.refundTransaction(validTransactionRefundRequest))
                .expectError(TransactionNotFoundException.class)
                .verify();

        verify(validationService, times(1)).validate(validTransactionRefundRequest);
        verify(transactionRepository, times(1)).findByOrderId(validTransactionRefundRequest.getOrderId());
        verify(balanceRepository, never()).findByCustomerId(anyLong());
        verify(balanceRepository, never()).save(any(Balance.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    public void testRefundTransactionCustomerNotFound() {
        when(validationService.validate(any(TransactionRefundRequest.class))).thenReturn(Mono.just(validTransactionRefundRequest));
        when(transactionRepository.findByOrderId(validTransactionRefundRequest.getOrderId())).thenReturn(Mono.just(transaction));
        when(balanceRepository.findByCustomerId(validTransactionRefundRequest.getCustomerId())).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.refundTransaction(validTransactionRefundRequest))
                .expectError(CustomerNotFoundException.class)
                .verify();

        verify(validationService, times(1)).validate(validTransactionRefundRequest);
        verify(transactionRepository, times(1)).findByOrderId(validTransactionRefundRequest.getOrderId());
        verify(balanceRepository, times(1)).findByCustomerId(validTransactionRefundRequest.getCustomerId());
        verify(balanceRepository, never()).save(any(Balance.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
