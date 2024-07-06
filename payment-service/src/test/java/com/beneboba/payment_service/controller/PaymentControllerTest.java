package com.beneboba.payment_service.controller;

import com.beneboba.payment_service.entity.Balance;
import com.beneboba.payment_service.entity.PaymentStatus;
import com.beneboba.payment_service.entity.Transaction;
import com.beneboba.payment_service.model.TransactionRefundRequest;
import com.beneboba.payment_service.model.TransactionRequest;
import com.beneboba.payment_service.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebFluxTest(PaymentController.class)
@Slf4j
public class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaymentService paymentService;

    private Transaction transaction;
    private Balance balance;
    private TransactionRequest transactionRequest;
    private TransactionRefundRequest transactionRefundRequest;

    @BeforeEach
    public void setup() {

        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setOrderId(1L);
        transaction.setAmount(100f);
        transaction.setMode("BCA");
        transaction.setStatus(PaymentStatus.COMPLETED);
        transaction.setReferenceNumber("12345678");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String formattedDateTime = formatter.format(now);
        transaction.setPaymentDate(LocalDateTime.parse(formattedDateTime, formatter));

        balance = new Balance();
        balance.setId(1L);
        balance.setCustomerId(1L);
        balance.setBalance(1000f);

        transactionRequest = new TransactionRequest();
        transactionRequest.setOrderId(1L);
        transactionRequest.setCustomerId(1L);
        transactionRequest.setAmount(100f);
        transactionRequest.setPaymentMethod("BCA");

        transactionRefundRequest = new TransactionRefundRequest();
        transactionRefundRequest.setOrderId(1L);
        transactionRefundRequest.setCustomerId(1L);
    }

    @Test
    public void checkFormatDate(){
        log.info("transaction :: {}",transaction);
    }

    @Test
    public void testGetAllTransaction() {
        when(paymentService.getAllTransaction()).thenReturn(Flux.just(transaction));

        webTestClient.get().uri("/api/payments")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();

        verify(paymentService, times(1)).getAllTransaction();
        }


    @Test
    public void testGetAllCustomerBalance() {
        when(paymentService.getAllCustomerBalance()).thenReturn(Flux.just(balance));

        webTestClient.get().uri("/api/payments/balance")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Balance.class)
                .hasSize(1);

        verify(paymentService, times(1)).getAllCustomerBalance();
    }

    @Test
    public void testCreateTransaction() {
        when(paymentService.createNewTransaction(any(TransactionRequest.class))).thenReturn(Mono.just(transaction));

        webTestClient.post().uri("/api/payments/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transactionRequest)
                .exchange()
                .expectStatus().isCreated();

        verify(paymentService, times(1)).createNewTransaction(any(TransactionRequest.class));
    }

    @Test
    public void testRefundTransaction() {
        when(paymentService.refundTransaction(any(TransactionRefundRequest.class))).thenReturn(Mono.just(transaction));

        webTestClient.patch().uri("/api/payments/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(transactionRefundRequest)
                .exchange()
                .expectStatus().isOk();

        verify(paymentService, times(1)).refundTransaction(transactionRefundRequest);
    }
}
