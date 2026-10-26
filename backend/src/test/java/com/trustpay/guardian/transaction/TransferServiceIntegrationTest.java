package com.trustpay.guardian.transaction;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountService;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.common.exception.DailyLimitExceededException;
import com.trustpay.guardian.common.exception.InsufficientFundsException;
import com.trustpay.guardian.fraud.AlertStatus;
import com.trustpay.guardian.fraud.FraudAlert;
import com.trustpay.guardian.fraud.FraudAlertRepository;
import com.trustpay.guardian.fraud.FraudAnalystService;
import com.trustpay.guardian.fraud.dto.AlertDecisionRequest;
import com.trustpay.guardian.fraud.dto.FraudAlertResponse;
import com.trustpay.guardian.transaction.dto.TransactionResponse;
import com.trustpay.guardian.transaction.dto.TransferRequest;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TransferServiceIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private FraudAnalystService fraudAnalystService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FraudAlertRepository fraudAlertRepository;

    @Autowired
    private com.trustpay.guardian.fraud.FraudRuleConfigService fraudRuleConfigService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        fraudRuleConfigService.clearCache();
    }

    @Test
    @DisplayName("Normal transfer between accounts updates balances atomically with APPROVED_SIMULATED status")
    void normalTransferSucceeds() {
        User sender = userRepository.save(new User("Sender One", "sender1@example.com", "9111111111", "hash", Role.CUSTOMER));
        User recipient = userRepository.save(new User("Recipient One", "recip1@example.com", "9111111112", "hash", Role.CUSTOMER));

        Account src = accountRepository.save(new Account(sender, "TPG_TX_01", AccountType.SAVINGS, new BigDecimal("25000.00"), new BigDecimal("100000.00")));
        Account dst = accountRepository.save(new Account(recipient, "TPG_TX_02", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("100000.00")));

        TransferRequest req = new TransferRequest(src.getId(), dst.getAccountNumber(), new BigDecimal("3000.00"), "Rent payment");
        String idempotencyKey = UUID.randomUUID().toString();

        TransactionResponse res = transferService.initiateTransfer(req, idempotencyKey, sender.getId(), "127.0.0.1", "REQ-TX-01");

        assertThat(res.getStatus()).isEqualTo(TransactionStatus.APPROVED_SIMULATED);
        assertThat(res.getAmount()).isEqualByComparingTo("3000.00");

        Account updatedSrc = accountRepository.findById(src.getId()).orElseThrow();
        Account updatedDst = accountRepository.findById(dst.getId()).orElseThrow();

        assertThat(updatedSrc.getBalance()).isEqualByComparingTo("22000.00");
        assertThat(updatedDst.getBalance()).isEqualByComparingTo("8000.00");

        // Repeating with same idempotency key returns identical transaction without double-debiting
        TransactionResponse duplicateRes = transferService.initiateTransfer(req, idempotencyKey, sender.getId(), "127.0.0.1", "REQ-TX-01-DUP");
        assertThat(duplicateRes.getReferenceId()).isEqualTo(res.getReferenceId());

        Account reCheckedSrc = accountRepository.findById(src.getId()).orElseThrow();
        assertThat(reCheckedSrc.getBalance()).isEqualByComparingTo("22000.00");
    }

    @Test
    @DisplayName("Transfer exceeding available balance throws InsufficientFundsException")
    void transferExceedingBalanceThrows() {
        User sender = userRepository.save(new User("Sender Two", "sender2@example.com", "9111111113", "hash", Role.CUSTOMER));
        User recipient = userRepository.save(new User("Recipient Two", "recip2@example.com", "9111111114", "hash", Role.CUSTOMER));

        Account src = accountRepository.save(new Account(sender, "TPG_TX_03", AccountType.SAVINGS, new BigDecimal("1000.00"), new BigDecimal("100000.00")));
        Account dst = accountRepository.save(new Account(recipient, "TPG_TX_04", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("100000.00")));

        TransferRequest req = new TransferRequest(src.getId(), dst.getAccountNumber(), new BigDecimal("1500.00"), "Overdraft attempt");

        assertThatThrownBy(() -> transferService.initiateTransfer(req, null, sender.getId(), "127.0.0.1", "REQ-TX-02"))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    @DisplayName("Transfer exceeding daily limit throws DailyLimitExceededException")
    void transferExceedingDailyLimitThrows() {
        User sender = userRepository.save(new User("Sender Three", "sender3@example.com", "9111111115", "hash", Role.CUSTOMER));
        User recipient = userRepository.save(new User("Recipient Three", "recip3@example.com", "9111111116", "hash", Role.CUSTOMER));

        Account src = accountRepository.save(new Account(sender, "TPG_TX_05", AccountType.SAVINGS, new BigDecimal("200000.00"), new BigDecimal("50000.00")));
        Account dst = accountRepository.save(new Account(recipient, "TPG_TX_06", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("100000.00")));

        TransferRequest req = new TransferRequest(src.getId(), dst.getAccountNumber(), new BigDecimal("60000.00"), "Exceed limit attempt");

        assertThatThrownBy(() -> transferService.initiateTransfer(req, null, sender.getId(), "127.0.0.1", "REQ-TX-03"))
                .isInstanceOf(DailyLimitExceededException.class);
    }

    @Test
    @DisplayName("Held transfer holds funds dynamically; analyst resolution posts or releases funds")
    void heldTransferAndAnalystResolution() {
        User sender = userRepository.save(new User("Sender Four", "sender4@example.com", "9111111117", "hash", Role.CUSTOMER));
        User recipient = userRepository.save(new User("Recipient Four", "recip4@example.com", "9111111118", "hash", Role.CUSTOMER));
        User analyst = userRepository.save(new User("Analyst Four", "analyst4@example.com", "9111111119", "hash", Role.FRAUD_ANALYST));

        Account src = accountRepository.save(new Account(sender, "TPG_TX_07", AccountType.SAVINGS, new BigDecimal("120000.00"), new BigDecimal("60000.00")));
        Account dst = accountRepository.save(new Account(recipient, "TPG_TX_08", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("200000.00")));

        // Amount >= 50,000 triggers R1 (Score >= 40), causing PENDING_REVIEW
        TransferRequest req = new TransferRequest(src.getId(), dst.getAccountNumber(), new BigDecimal("55000.00"), "Large equipment purchase");
        TransactionResponse txRes = transferService.initiateTransfer(req, null, sender.getId(), "127.0.0.1", "REQ-HELD-01");

        assertThat(txRes.getStatus()).isEqualTo(TransactionStatus.PENDING_REVIEW);

        // Ledger balance unchanged (120,000.00), but Available Balance dynamically holds 55,000.00
        Account srcAfterHold = accountRepository.findById(src.getId()).orElseThrow();
        assertThat(srcAfterHold.getBalance()).isEqualByComparingTo("120000.00");

        BigDecimal availableBalance = accountService.calculateAvailableBalance(srcAfterHold);
        assertThat(availableBalance).isEqualByComparingTo("65000.00"); // 120,000 - 55,000

        // Locate created alert
        FraudAlert alert = fraudAlertRepository.findByTransactionId(txRes.getId()).orElseThrow();
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);

        // Analyst approves & releases the transaction
        AlertDecisionRequest decision = new AlertDecisionRequest("RELEASE", "Customer verified via phone call OTP");
        FraudAlertResponse resolvedAlert = fraudAnalystService.resolveAlert(
                alert.getId(), decision, analyst.getId(), "127.0.0.1", "REQ-ANALYST-01");

        assertThat(resolvedAlert.getStatus()).isEqualTo(AlertStatus.RESOLVED_RELEASED);

        // After release, ledger balances are posted
        Account srcAfterRelease = accountRepository.findById(src.getId()).orElseThrow();
        Account dstAfterRelease = accountRepository.findById(dst.getId()).orElseThrow();

        assertThat(srcAfterRelease.getBalance()).isEqualByComparingTo("65000.00");
        assertThat(dstAfterRelease.getBalance()).isEqualByComparingTo("65000.00"); // 10,000 + 55,000
    }
}
