package com.trustpay.guardian;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountStatus;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.audit.AuditAction;
import com.trustpay.guardian.audit.AuditLog;
import com.trustpay.guardian.audit.AuditLogRepository;
import com.trustpay.guardian.audit.AuditTargetType;
import com.trustpay.guardian.auth.RefreshToken;
import com.trustpay.guardian.auth.RefreshTokenRepository;
import com.trustpay.guardian.beneficiary.Beneficiary;
import com.trustpay.guardian.beneficiary.BeneficiaryRepository;
import com.trustpay.guardian.fraud.AlertStatus;
import com.trustpay.guardian.fraud.FraudAlert;
import com.trustpay.guardian.fraud.FraudAlertHit;
import com.trustpay.guardian.fraud.FraudAlertHitRepository;
import com.trustpay.guardian.fraud.FraudAlertRepository;
import com.trustpay.guardian.fraud.RiskLevel;
import com.trustpay.guardian.notification.Notification;
import com.trustpay.guardian.notification.NotificationRepository;
import com.trustpay.guardian.notification.NotificationType;
import com.trustpay.guardian.transaction.Transaction;
import com.trustpay.guardian.transaction.TransactionRepository;
import com.trustpay.guardian.transaction.TransactionStatus;
import com.trustpay.guardian.transaction.TransactionType;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import com.trustpay.guardian.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EntityAndRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FraudAlertRepository fraudAlertRepository;

    @Autowired
    private FraudAlertHitRepository fraudAlertHitRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("Persist and query User entity with security state")
    void persistAndQueryUser() {
        User user = new User("Rahul Sharma", "rahul.sharma@example.com", "9876543210", "bcrypt_hash_placeholder", Role.CUSTOMER);
        user = userRepository.save(user);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(0);

        Optional<User> found = userRepository.findByEmail("rahul.sharma@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Rahul Sharma");

        // Verify pessimistic lock query executes cleanly
        Optional<User> locked = userRepository.findByIdForUpdate(user.getId());
        assertThat(locked).isPresent();
    }

    @Test
    @DisplayName("Persist and query Account entity with decimal precision and user link")
    void persistAndQueryAccount() {
        User user = userRepository.save(new User("Priya Patel", "priya.patel@example.com", "9876543211", "bcrypt_hash", Role.CUSTOMER));
        Account account = new Account(user, "TPG100000000001", AccountType.SAVINGS, new BigDecimal("25000.50"), new BigDecimal("100000.00"));
        account = accountRepository.save(account);

        assertThat(account.getId()).isNotNull();
        assertThat(account.getBalance()).isEqualByComparingTo("25000.50");
        assertThat(account.getDailyLimit()).isEqualByComparingTo("100000.00");
        assertThat(account.getCurrency()).isEqualTo("INR");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);

        Optional<Account> found = accountRepository.findByAccountNumber("TPG100000000001");
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("priya.patel@example.com");

        // Test count by user id
        assertThat(accountRepository.countByUserId(user.getId())).isEqualTo(1);

        // Test pessimistic lock query
        Optional<Account> locked = accountRepository.findByIdForUpdate(account.getId());
        assertThat(locked).isPresent();
    }

    @Test
    @DisplayName("CHECK constraint rejects negative account balance")
    void rejectNegativeAccountBalance() {
        User user = userRepository.save(new User("Vikram Malhotra", "vikram.m@example.com", "9876543299", "bcrypt_hash", Role.CUSTOMER));
        Account account = new Account(user, "TPG999999999999", AccountType.SAVINGS, new BigDecimal("-50.00"), new BigDecimal("100000.00"));

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            accountRepository.saveAndFlush(account);
        });
    }

    @Test
    @DisplayName("Persist and query Beneficiary with compound uniqueness")
    void persistAndQueryBeneficiary() {
        User sender = userRepository.save(new User("Amit Kumar", "amit.kumar@example.com", "9876543212", "bcrypt_hash", Role.CUSTOMER));
        User recipient = userRepository.save(new User("Neha Singh", "neha.singh@example.com", "9876543213", "bcrypt_hash", Role.CUSTOMER));
        Account recipientAccount = accountRepository.save(new Account(recipient, "TPG100000000002", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00")));

        Beneficiary beneficiary = new Beneficiary(sender, recipientAccount, "Neha Work");
        beneficiary = beneficiaryRepository.save(beneficiary);

        assertThat(beneficiary.getId()).isNotNull();
        assertThat(beneficiary.getNickname()).isEqualTo("Neha Work");
        assertThat(beneficiaryRepository.existsByUserIdAndRecipientAccountId(sender.getId(), recipientAccount.getId())).isTrue();
    }

    @Test
    @DisplayName("Persist and query Transaction with idempotency and reference key")
    void persistAndQueryTransaction() {
        User sender = userRepository.save(new User("Vikram Malhotra", "vikram@example.com", "9876543214", "bcrypt_hash", Role.CUSTOMER));
        User recipient = userRepository.save(new User("Ananya Roy", "ananya@example.com", "9876543215", "bcrypt_hash", Role.CUSTOMER));

        Account srcAccount = accountRepository.save(new Account(sender, "TPG100000000003", AccountType.SAVINGS, new BigDecimal("50000.00"), new BigDecimal("100000.00")));
        Account dstAccount = accountRepository.save(new Account(recipient, "TPG100000000004", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00")));

        Transaction txn = new Transaction("TXN-TESTREF12345", "uuid-idempotency-key-001", srcAccount, dstAccount,
                new BigDecimal("15000.00"), TransactionType.TRANSFER, TransactionStatus.PENDING_REVIEW);
        txn.setRiskScore(45);
        txn = transactionRepository.save(txn);

        assertThat(txn.getId()).isNotNull();
        assertThat(txn.getReferenceId()).isEqualTo("TXN-TESTREF12345");
        assertThat(txn.getIdempotencyKey()).isEqualTo("uuid-idempotency-key-001");
        assertThat(txn.getAmount()).isEqualByComparingTo("15000.00");
        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.PENDING_REVIEW);

        Optional<Transaction> byKey = transactionRepository.findByIdempotencyKey("uuid-idempotency-key-001");
        assertThat(byKey).isPresent();
        assertThat(byKey.get().getId()).isEqualTo(txn.getId());
    }

    @Test
    @DisplayName("Persist FraudAlert with 1-to-1 link to Transaction and FraudAlertHits")
    void persistFraudAlertAndHits() {
        User customer = userRepository.save(new User("Deepak Verma", "deepak@example.com", "9876543216", "bcrypt_hash", Role.CUSTOMER));
        User analyst = userRepository.save(new User("Analyst One", "analyst1@trustpay.internal", "9876543217", "bcrypt_hash", Role.FRAUD_ANALYST));

        Account src = accountRepository.save(new Account(customer, "TPG100000000005", AccountType.SAVINGS, new BigDecimal("60000.00"), new BigDecimal("100000.00")));
        Account dst = accountRepository.save(new Account(customer, "TPG100000000006", AccountType.CURRENT, new BigDecimal("10000.00"), new BigDecimal("100000.00")));

        Transaction txn = transactionRepository.save(new Transaction("TXN-ALERT123456", "uuid-alert-001", src, dst,
                new BigDecimal("55000.00"), TransactionType.TRANSFER, TransactionStatus.PENDING_REVIEW));

        FraudAlert alert = new FraudAlert(txn, 55, RiskLevel.MEDIUM);
        alert.setAssignedTo(analyst);
        alert = fraudAlertRepository.save(alert);

        assertThat(alert.getId()).isNotNull();
        assertThat(alert.getTransaction().getId()).isEqualTo(txn.getId());
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);

        FraudAlertHit hit1 = fraudAlertHitRepository.save(new FraudAlertHit(alert, "R1", 30, "Amount ₹55,000 exceeds ₹50,000 threshold"));
        FraudAlertHit hit2 = fraudAlertHitRepository.save(new FraudAlertHit(alert, "R2", 25, "Velocity threshold exceeded"));

        var hits = fraudAlertHitRepository.findByAlertId(alert.getId());
        assertThat(hits).hasSize(2);
        assertThat(hits).extracting(FraudAlertHit::getRuleCode).containsExactlyInAnyOrder("R1", "R2");
    }

    @Test
    @DisplayName("Persist and query RefreshToken with rotation hash pointer")
    void persistRefreshToken() {
        User user = userRepository.save(new User("Kavita Das", "kavita@example.com", "9876543218", "bcrypt_hash", Role.CUSTOMER));
        Instant expires = Instant.now().plus(7, ChronoUnit.DAYS);

        RefreshToken token = new RefreshToken(user, "sha256_hash_token_abc123", expires);
        token = refreshTokenRepository.save(token);

        assertThat(token.getId()).isNotNull();
        assertThat(token.getTokenHash()).isEqualTo("sha256_hash_token_abc123");

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("sha256_hash_token_abc123");
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("Persist Notification and query unread count")
    void persistNotificationAndCount() {
        User user = userRepository.save(new User("Sanjay Gupta", "sanjay@example.com", "9876543219", "bcrypt_hash", Role.CUSTOMER));

        notificationRepository.save(new Notification(user, NotificationType.TRANSFER_RECEIVED, "Credit Alert", "You received ₹5,000.00"));
        notificationRepository.save(new Notification(user, NotificationType.TRANSFER_SENT, "Debit Alert", "You sent ₹1,200.00"));

        long unread = notificationRepository.countByUserIdAndReadAtIsNull(user.getId());
        assertThat(unread).isEqualTo(2);

        var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        assertThat(notifications).hasSize(2);
    }

    @Test
    @DisplayName("Persist immutable AuditLog record")
    void persistAuditLog() {
        User admin = userRepository.save(new User("Admin User", "admin@trustpay.internal", "9876543220", "bcrypt_hash", Role.ADMIN));

        AuditLog audit = new AuditLog(admin, AuditAction.USER_STATUS_CHANGE, AuditTargetType.USER, "123",
                "{\"old\":\"ACTIVE\",\"new\":\"SUSPENDED\"}", "127.0.0.1", "req-uuid-001");
        audit = auditLogRepository.save(audit);

        assertThat(audit.getId()).isNotNull();
        assertThat(audit.getAction()).isEqualTo(AuditAction.USER_STATUS_CHANGE);
        assertThat(audit.getTargetType()).isEqualTo(AuditTargetType.USER);

        var logs = auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(AuditTargetType.USER, "123");
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getDetailsJson()).contains("SUSPENDED");
    }
}
