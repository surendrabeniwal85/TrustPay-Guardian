package com.trustpay.guardian.config;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.beneficiary.Beneficiary;
import com.trustpay.guardian.beneficiary.BeneficiaryRepository;
import com.trustpay.guardian.notification.NotificationService;
import com.trustpay.guardian.notification.NotificationType;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Seeds deterministic personas for local development and demonstration.
 * Activated strictly in the "dev" profile.
 * Passwords are loaded from environment variable DEV_SEED_PASSWORD (via AppProperties).
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AppProperties appProperties;

    public DevDataSeeder(
            UserRepository userRepository,
            AccountRepository accountRepository,
            BeneficiaryRepository beneficiaryRepository,
            PasswordEncoder passwordEncoder,
            NotificationService notificationService,
            AppProperties appProperties) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail("admin@trustpay.internal") || userRepository.existsByEmail("admin@trustpay.com")) {
            log.info("Baseline demo seed data already present in database.");
            return;
        }

        String rawSeedPassword = appProperties.getSeed().getPassword();
        if (rawSeedPassword == null || rawSeedPassword.isBlank()) {
            rawSeedPassword = "DevSecurePassword#2026!";
        }
        String seedPasswordHash = passwordEncoder.encode(rawSeedPassword);

        log.info("Seeding baseline demo personas (1 admin, 1 analyst, 3 customers) for dev profile...");

        // 1. Administrator
        User admin = new User(
                "System Administrator",
                "admin@trustpay.internal",
                "9999900001",
                seedPasswordHash,
                Role.ADMIN
        );
        userRepository.save(admin);

        // 2. Fraud Analyst
        User analyst = new User(
                "Senior Fraud Analyst",
                "analyst@trustpay.internal",
                "9999900002",
                seedPasswordHash,
                Role.FRAUD_ANALYST
        );
        userRepository.save(analyst);

        // 3. Customer 1 (John Doe)
        User cust1 = new User(
                "John Doe",
                "john.doe@example.com",
                "9876543210",
                seedPasswordHash,
                Role.CUSTOMER
        );
        cust1 = userRepository.save(cust1);

        Account acc1 = new Account(
                cust1,
                "TPG100000000001",
                AccountType.SAVINGS,
                new BigDecimal("50000.00"),
                new BigDecimal("100000.00")
        );
        acc1 = accountRepository.save(acc1);

        // 4. Customer 2 (Jane Smith)
        User cust2 = new User(
                "Jane Smith",
                "jane.smith@example.com",
                "9876543211",
                seedPasswordHash,
                Role.CUSTOMER
        );
        cust2 = userRepository.save(cust2);

        Account acc2 = new Account(
                cust2,
                "TPG100000000002",
                AccountType.SAVINGS,
                new BigDecimal("30000.00"),
                new BigDecimal("100000.00")
        );
        acc2 = accountRepository.save(acc2);

        // 5. Customer 3 (Bob Johnson)
        User cust3 = new User(
                "Bob Johnson",
                "bob.johnson@example.com",
                "9876543212",
                seedPasswordHash,
                Role.CUSTOMER
        );
        cust3 = userRepository.save(cust3);

        Account acc3 = new Account(
                cust3,
                "TPG100000000003",
                AccountType.SAVINGS,
                new BigDecimal("20000.00"),
                new BigDecimal("100000.00")
        );
        acc3 = accountRepository.save(acc3);

        // Seed Beneficiaries among customers
        Beneficiary b1 = new Beneficiary(cust1, acc2, "Jane Smith (Savings)");
        Beneficiary b2 = new Beneficiary(cust1, acc3, "Bob Johnson (Savings)");
        Beneficiary b3 = new Beneficiary(cust2, acc1, "John Doe (Personal)");
        beneficiaryRepository.save(b1);
        beneficiaryRepository.save(b2);
        beneficiaryRepository.save(b3);

        // Welcome notifications
        notificationService.sendNotification(
                cust1,
                NotificationType.ACCOUNT_CREATED,
                "Welcome to TrustPay Guardian",
                "Your account TPG******0001 is active with a demo balance of ₹50,000.00."
        );
        notificationService.sendNotification(
                cust2,
                NotificationType.ACCOUNT_CREATED,
                "Welcome to TrustPay Guardian",
                "Your account TPG******0002 is active with a demo balance of ₹30,000.00."
        );
        notificationService.sendNotification(
                cust3,
                NotificationType.ACCOUNT_CREATED,
                "Welcome to TrustPay Guardian",
                "Your account TPG******0003 is active with a demo balance of ₹20,000.00."
        );

        log.info("Dev demo data initialization complete: 1 admin, 1 analyst, 3 customers seeded.");
    }
}
