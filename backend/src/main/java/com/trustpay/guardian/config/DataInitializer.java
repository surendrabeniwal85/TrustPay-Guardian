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
import com.trustpay.guardian.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public DataInitializer(
            UserRepository userRepository,
            AccountRepository accountRepository,
            BeneficiaryRepository beneficiaryRepository,
            PasswordEncoder passwordEncoder,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail("admin@trustpay.com")) {
            log.info("Baseline demo users and accounts already initialized.");
            return;
        }

        log.info("Initializing baseline demo users, accounts, and beneficiaries...");

        // 1. Admin
        User admin = new User(
                "System Administrator",
                "admin@trustpay.com",
                "9999900001",
                passwordEncoder.encode("Admin@123"),
                Role.ADMIN
        );
        userRepository.save(admin);

        // 2. Fraud Analyst
        User analyst = new User(
                "Senior Fraud Analyst",
                "analyst@trustpay.com",
                "9999900002",
                passwordEncoder.encode("Analyst@123"),
                Role.FRAUD_ANALYST
        );
        userRepository.save(analyst);

        // 3. Customer 1 (John Doe)
        User customer1 = new User(
                "John Doe",
                "john.doe@example.com",
                "9876543210",
                passwordEncoder.encode("Customer@123"),
                Role.CUSTOMER
        );
        User savedCust1 = userRepository.save(customer1);

        Account account1 = new Account(
                savedCust1,
                "TPG100000000001",
                AccountType.SAVINGS,
                new BigDecimal("50000.00"),
                new BigDecimal("100000.00")
        );
        Account savedAcc1 = accountRepository.save(account1);

        // 4. Customer 2 (Jane Smith)
        User customer2 = new User(
                "Jane Smith",
                "jane.smith@example.com",
                "9876543211",
                passwordEncoder.encode("Customer@123"),
                Role.CUSTOMER
        );
        User savedCust2 = userRepository.save(customer2);

        Account account2 = new Account(
                savedCust2,
                "TPG100000000002",
                AccountType.SAVINGS,
                new BigDecimal("30000.00"),
                new BigDecimal("100000.00")
        );
        Account savedAcc2 = accountRepository.save(account2);

        // 5. Seed Mutual Beneficiaries
        Beneficiary b1 = new Beneficiary(savedCust1, savedAcc2, "Jane Smith (Savings)");
        beneficiaryRepository.save(b1);

        Beneficiary b2 = new Beneficiary(savedCust2, savedAcc1, "John Doe (Personal)");
        beneficiaryRepository.save(b2);

        // 6. Welcome notifications
        notificationService.sendNotification(
                savedCust1,
                NotificationType.ACCOUNT_CREATED,
                "Welcome to TrustPay Guardian",
                "Your account TPG******0001 is active with an opening demo balance of ₹50,000.00."
        );

        notificationService.sendNotification(
                savedCust2,
                NotificationType.ACCOUNT_CREATED,
                "Welcome to TrustPay Guardian",
                "Your account TPG******0002 is active with an opening demo balance of ₹30,000.00."
        );

        log.info("Baseline demo data initialization completed successfully.");
    }
}
