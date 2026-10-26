package com.trustpay.guardian.transaction;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.common.exception.InsufficientFundsException;
import com.trustpay.guardian.transaction.dto.TransactionResponse;
import com.trustpay.guardian.transaction.dto.TransferRequest;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TransferConcurrencyIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("20 parallel concurrent transfers strictly enforce zero overdrawing and system money conservation")
    void twentyParallelTransfersConserveMoneyAndPreventOverdrawing() throws InterruptedException {
        runConcurrencyScenario();
    }

    @RepeatedTest(value = 5, name = "Repeat {currentRepetition} of {totalRepetitions} - Concurrency stress test")
    @DisplayName("Concurrency test passes repeatedly (5 consecutive runs without race or deadlock)")
    void repeatedConcurrencyStressTest() throws InterruptedException {
        runConcurrencyScenario();
    }

    private void runConcurrencyScenario() throws InterruptedException {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        User sender = userRepository.save(new User(
                "Concurrent Sender " + uniqueSuffix,
                "sender." + uniqueSuffix + "@example.com",
                "98" + uniqueSuffix.replaceAll("[^0-9]", "0").substring(0, Math.min(8, uniqueSuffix.length())),
                "passwordHash123",
                Role.CUSTOMER
        ));

        User recipient = userRepository.save(new User(
                "Concurrent Recipient " + uniqueSuffix,
                "recipient." + uniqueSuffix + "@example.com",
                "97" + uniqueSuffix.replaceAll("[^0-9]", "0").substring(0, Math.min(8, uniqueSuffix.length())),
                "passwordHash123",
                Role.CUSTOMER
        ));

        // Initial setup: Sender has ₹10,000.00, Recipient has ₹5,000.00. Total money = ₹15,000.00.
        BigDecimal senderInitialBalance = new BigDecimal("10000.00");
        BigDecimal recipientInitialBalance = new BigDecimal("5000.00");
        BigDecimal totalSystemMoneyExpected = senderInitialBalance.add(recipientInitialBalance);

        Account src = accountRepository.save(new Account(
                sender, "TPG" + System.nanoTime(), AccountType.SAVINGS, senderInitialBalance, new BigDecimal("200000.00")));
        Account dst = accountRepository.save(new Account(
                recipient, "TPG" + (System.nanoTime() + 1), AccountType.SAVINGS, recipientInitialBalance, new BigDecimal("200000.00")));

        int threadCount = 20;
        BigDecimal transferAmount = new BigDecimal("1000.00");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientFundsCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronized release of all 20 threads simultaneously
                    TransferRequest req = new TransferRequest(src.getId(), dst.getAccountNumber(), transferAmount, "Concurrency test transfer");
                    String idempotencyKey = UUID.randomUUID().toString();
                    TransactionResponse res = transferService.initiateTransfer(
                            req, idempotencyKey, sender.getId(), "127.0.0.1", "REQ-CONCURRENCY");

                    if (res.getStatus() == TransactionStatus.APPROVED_SIMULATED) {
                        successCount.incrementAndGet();
                    }
                } catch (InsufficientFundsException ex) {
                    insufficientFundsCount.incrementAndGet();
                } catch (Exception ex) {
                    System.err.println("Unexpected concurrency error: " + ex.getClass().getName() + " -> " + ex.getMessage());
                    ex.printStackTrace();
                    unexpectedErrorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        boolean completedInTime = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completedInTime).as("All 20 concurrent threads must complete within timeout").isTrue();
        assertThat(unexpectedErrorCount.get()).as("Zero unexpected errors during concurrency execution").isEqualTo(0);

        // Verification of invariant state
        Account finalSrc = accountRepository.findById(src.getId()).orElseThrow();
        Account finalDst = accountRepository.findById(dst.getId()).orElseThrow();

        // 1. Sender account must NEVER be overdrawn (balance >= 0)
        assertThat(finalSrc.getBalance().compareTo(BigDecimal.ZERO))
                .as("Sender account balance must never be negative")
                .isGreaterThanOrEqualTo(0);

        // 2. Exactly 10 transfers should have succeeded and 10 failed
        assertThat(successCount.get())
                .as("Exactly 10 transfers should succeed from ₹10,000.00 balance with ₹1,000.00 transfers")
                .isEqualTo(10);
        assertThat(insufficientFundsCount.get())
                .as("Remaining 10 transfers should be rejected with InsufficientFundsException")
                .isEqualTo(10);

        // 3. Sender balance must be exactly ₹0.00
        assertThat(finalSrc.getBalance())
                .as("Sender balance after 10 transfers of ₹1,000 must be ₹0.00")
                .isEqualByComparingTo("0.00");

        // 4. Recipient balance must be ₹15,000.00 (₹5,000 initial + 10 * ₹1,000)
        assertThat(finalDst.getBalance())
                .as("Recipient balance must equal initial balance plus total debited amount")
                .isEqualByComparingTo("15000.00");

        // 5. Total money in the system must be strictly conserved
        BigDecimal totalSystemMoneyActual = finalSrc.getBalance().add(finalDst.getBalance());
        assertThat(totalSystemMoneyActual)
                .as("Total money in system must remain invariant (₹15,000.00)")
                .isEqualByComparingTo(totalSystemMoneyExpected);
    }
}
