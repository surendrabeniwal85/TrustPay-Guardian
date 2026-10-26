package com.trustpay.guardian;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.transaction.Transaction;
import com.trustpay.guardian.transaction.TransactionRepository;
import com.trustpay.guardian.transaction.TransactionStatus;
import com.trustpay.guardian.transaction.TransactionType;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BalanceReservationAndStateTransitionTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private BigDecimal calculateAvailableBalance(Account account) {
        BigDecimal pendingSum = transactionRepository.sumPendingOutgoingTransfers(account.getId());
        return account.getBalance().subtract(pendingSum);
    }

    @Test
    @DisplayName("Available balance accurately reserves funds for PENDING_REVIEW without modifying ledger balance")
    void pendingReviewReservesFunds() {
        User sender = userRepository.save(new User("Sender User", "sender1@example.com", "9876543231", "hash", Role.CUSTOMER));
        User recipient = userRepository.save(new User("Recipient User", "recipient1@example.com", "9876543232", "hash", Role.CUSTOMER));

        Account srcAccount = accountRepository.save(new Account(sender, "TPG200000000001", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00")));
        Account dstAccount = accountRepository.save(new Account(recipient, "TPG200000000002", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("100000.00")));

        // Initially: Balance = 10,000.00, Pending = 0.00, Available = 10,000.00
        assertThat(srcAccount.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(calculateAvailableBalance(srcAccount)).isEqualByComparingTo("10000.00");

        // Transaction 1: ₹2,000.00 enters PENDING_REVIEW (e.g. Risk score 45)
        Transaction txn1 = transactionRepository.save(new Transaction("TXN-PENDING-001", "uuid-res-001", srcAccount, dstAccount,
                new BigDecimal("2000.00"), TransactionType.TRANSFER, TransactionStatus.PENDING_REVIEW));

        // Ledger balance remains 10,000.00; Available balance decreases to 8,000.00
        Account refreshedSrc = accountRepository.findById(srcAccount.getId()).orElseThrow();
        assertThat(refreshedSrc.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(calculateAvailableBalance(refreshedSrc)).isEqualByComparingTo("8000.00");

        // Transaction 2: ₹1,500.00 also enters PENDING_REVIEW
        Transaction txn2 = transactionRepository.save(new Transaction("TXN-PENDING-002", "uuid-res-002", srcAccount, dstAccount,
                new BigDecimal("1500.00"), TransactionType.TRANSFER, TransactionStatus.PENDING_REVIEW));

        // Total pending = 3,500.00; Available balance = 6,500.00; Ledger balance intact at 10,000.00
        assertThat(calculateAvailableBalance(refreshedSrc)).isEqualByComparingTo("6500.00");
    }

    @Test
    @DisplayName("Analyst RELEASE transitions PENDING_REVIEW to APPROVED_SIMULATED and converts reservation to posted debit")
    void analystReleaseConvertsReservationToPostedDebit() {
        User sender = userRepository.save(new User("Sender User 2", "sender2@example.com", "9876543233", "hash", Role.CUSTOMER));
        User recipient = userRepository.save(new User("Recipient User 2", "recipient2@example.com", "9876543234", "hash", Role.CUSTOMER));

        Account src = accountRepository.save(new Account(sender, "TPG200000000003", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00")));
        Account dst = accountRepository.save(new Account(recipient, "TPG200000000004", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("100000.00")));

        Transaction txn = transactionRepository.save(new Transaction("TXN-REL-001", "uuid-rel-001", src, dst,
                new BigDecimal("3000.00"), TransactionType.TRANSFER, TransactionStatus.PENDING_REVIEW));

        // Available balance is 7,000.00, ledger balance is 10,000.00
        assertThat(calculateAvailableBalance(src)).isEqualByComparingTo("7000.00");

        // Analyst RELEASE: Debit sender, credit recipient, mark APPROVED_SIMULATED
        src.setBalance(src.getBalance().subtract(txn.getAmount()));
        dst.setBalance(dst.getBalance().add(txn.getAmount()));
        accountRepository.save(src);
        accountRepository.save(dst);

        txn.setStatus(TransactionStatus.APPROVED_SIMULATED);
        txn.setCompletedAt(Instant.now());
        transactionRepository.save(txn);

        // Verification:
        // Sender ledger balance is posted as 7,000.00
        // Pending outgoing transfers is now 0.00
        // Available balance remains exactly 7,000.00 (NO double deduction)
        Account updatedSrc = accountRepository.findById(src.getId()).orElseThrow();
        Account updatedDst = accountRepository.findById(dst.getId()).orElseThrow();

        assertThat(updatedSrc.getBalance()).isEqualByComparingTo("7000.00");
        assertThat(calculateAvailableBalance(updatedSrc)).isEqualByComparingTo("7000.00");
        assertThat(updatedDst.getBalance()).isEqualByComparingTo("8000.00");
    }

    @Test
    @DisplayName("Analyst CONFIRM_BLOCK on PENDING_REVIEW transitions to BLOCKED and releases reservation exactly once")
    void analystBlockReleasesReservationExactlyOnce() {
        User sender = userRepository.save(new User("Sender User 3", "sender3@example.com", "9876543235", "hash", Role.CUSTOMER));
        User recipient = userRepository.save(new User("Recipient User 3", "recipient3@example.com", "9876543236", "hash", Role.CUSTOMER));

        Account src = accountRepository.save(new Account(sender, "TPG200000000005", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00")));
        Account dst = accountRepository.save(new Account(recipient, "TPG200000000006", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("100000.00")));

        Transaction txn = transactionRepository.save(new Transaction("TXN-BLK-001", "uuid-blk-001", src, dst,
                new BigDecimal("4000.00"), TransactionType.TRANSFER, TransactionStatus.PENDING_REVIEW));

        // While pending: Available = 6,000.00, Ledger = 10,000.00
        assertThat(calculateAvailableBalance(src)).isEqualByComparingTo("6000.00");
        assertThat(src.getBalance()).isEqualByComparingTo("10000.00");

        // Analyst CONFIRM_BLOCK: Transaction transitions from PENDING_REVIEW to BLOCKED
        txn.setStatus(TransactionStatus.BLOCKED);
        txn.setFailureReason("Confirmed fraudulent by analyst");
        transactionRepository.save(txn);

        // After block:
        // No money moved (Ledger balance remains 10,000.00)
        // Pending outgoing transfers is 0.00 (BLOCKED does not hold pending reservations)
        // Available balance is fully restored to 10,000.00 (reservation released exactly once)
        Account updatedSrc = accountRepository.findById(src.getId()).orElseThrow();
        Account updatedDst = accountRepository.findById(dst.getId()).orElseThrow();

        assertThat(updatedSrc.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(calculateAvailableBalance(updatedSrc)).isEqualByComparingTo("10000.00");
        assertThat(updatedDst.getBalance()).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("Transaction CANCELLATION releases reservation and restores available balance")
    void cancellationReleasesReservation() {
        User sender = userRepository.save(new User("Sender User 4", "sender4@example.com", "9876543237", "hash", Role.CUSTOMER));
        User recipient = userRepository.save(new User("Recipient User 4", "recipient4@example.com", "9876543238", "hash", Role.CUSTOMER));

        Account src = accountRepository.save(new Account(sender, "TPG200000000007", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00")));
        Account dst = accountRepository.save(new Account(recipient, "TPG200000000008", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("100000.00")));

        Transaction txn = transactionRepository.save(new Transaction("TXN-CAN-001", "uuid-can-001", src, dst,
                new BigDecimal("2500.00"), TransactionType.TRANSFER, TransactionStatus.PENDING_REVIEW));

        assertThat(calculateAvailableBalance(src)).isEqualByComparingTo("7500.00");

        // Customer or system cancels the held transfer
        txn.setStatus(TransactionStatus.CANCELLED);
        txn.setFailureReason("Cancelled by customer before analyst review");
        transactionRepository.save(txn);

        // Reservation immediately freed
        assertThat(calculateAvailableBalance(src)).isEqualByComparingTo("10000.00");
        assertThat(src.getBalance()).isEqualByComparingTo("10000.00");
    }
}
