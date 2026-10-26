package com.trustpay.guardian.fraud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.beneficiary.Beneficiary;
import com.trustpay.guardian.beneficiary.BeneficiaryRepository;
import com.trustpay.guardian.fraud.dto.AlertDecisionRequest;
import com.trustpay.guardian.fraud.dto.AlertNotesRequest;
import com.trustpay.guardian.security.JwtService;
import com.trustpay.guardian.security.UserPrincipal;
import com.trustpay.guardian.transaction.Transaction;
import com.trustpay.guardian.transaction.TransactionRepository;
import com.trustpay.guardian.transaction.TransactionStatus;
import com.trustpay.guardian.transaction.TransferService;
import com.trustpay.guardian.transaction.dto.TransferRequest;
import com.trustpay.guardian.transaction.dto.TransactionResponse;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FraudWorkflowAndRoleSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private TransferService transferService;

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
    private JwtService jwtService;

    @Autowired
    private FraudRuleConfigService fraudRuleConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User customer;
    private User analyst;
    private User admin;
    private User recipient;

    private Account customerAccount;
    private Account recipientAccount;

    private String customerToken;
    private String analystToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        fraudRuleConfigService.clearCache();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        customer = userRepository.save(new User("Customer User", "customer_fraud@test.com", "9000000021", "hash", Role.CUSTOMER));
        analyst = userRepository.save(new User("Fraud Analyst", "analyst_fraud@test.com", "9000000022", "hash", Role.FRAUD_ANALYST));
        admin = userRepository.save(new User("System Admin", "admin_fraud@test.com", "9000000023", "hash", Role.ADMIN));
        recipient = userRepository.save(new User("Recipient User", "recipient_fraud@test.com", "9000000024", "hash", Role.CUSTOMER));

        customerAccount = accountRepository.save(new Account(customer, "ACC-CUST-99", AccountType.SAVINGS, new BigDecimal("150000.00"), new BigDecimal("200000.00")));
        recipientAccount = accountRepository.save(new Account(recipient, "ACC-RECP-99", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("200000.00")));

        customerToken = "Bearer " + jwtService.generateAccessToken(UserPrincipal.create(customer));
        analystToken = "Bearer " + jwtService.generateAccessToken(UserPrincipal.create(analyst));
        adminToken = "Bearer " + jwtService.generateAccessToken(UserPrincipal.create(admin));
    }

    @Test
    @DisplayName("Check 1: Low-risk ₹500 transfer scores 0 and completes immediately as APPROVED_SIMULATED")
    void lowRiskTransferCompletesImmediately() {
        TransferRequest req = new TransferRequest(customerAccount.getId(), recipientAccount.getAccountNumber(), new BigDecimal("500.00"), "Lunch");

        TransactionResponse res = transferService.initiateTransfer(req, UUID.randomUUID().toString(), customer.getId(), "127.0.0.1", "REQ-01");

        assertThat(res.getStatus()).isEqualTo(TransactionStatus.APPROVED_SIMULATED);
        assertThat(res.getAmount()).isEqualByComparingTo("500.00");

        Account refreshedSrc = accountRepository.findById(customerAccount.getId()).orElseThrow();
        Account refreshedDst = accountRepository.findById(recipientAccount.getId()).orElseThrow();

        assertThat(refreshedSrc.getBalance()).isEqualByComparingTo("149500.00");
        assertThat(refreshedDst.getBalance()).isEqualByComparingTo("10500.00");

        assertThat(fraudAlertRepository.findByTransactionId(res.getId())).isEmpty();
    }

    @Test
    @DisplayName("Check 2: ₹60,000 transfer to newly added beneficiary (<24h) scores 60, held as PENDING_REVIEW, balance unchanged")
    void highAmountToNewBeneficiaryHeldForReview() {
        // Add new beneficiary (created right now)
        beneficiaryRepository.save(new Beneficiary(customer, recipientAccount, "New Colleague"));

        TransferRequest req = new TransferRequest(customerAccount.getId(), recipientAccount.getAccountNumber(), new BigDecimal("60000.00"), "Project fee");

        TransactionResponse res = transferService.initiateTransfer(req, UUID.randomUUID().toString(), customer.getId(), "127.0.0.1", "REQ-02");

        assertThat(res.getStatus()).isEqualTo(TransactionStatus.PENDING_REVIEW);

        Account refreshedSrc = accountRepository.findById(customerAccount.getId()).orElseThrow();
        Account refreshedDst = accountRepository.findById(recipientAccount.getId()).orElseThrow();

        // Balances untouched
        assertThat(refreshedSrc.getBalance()).isEqualByComparingTo("150000.00");
        assertThat(refreshedDst.getBalance()).isEqualByComparingTo("10000.00");

        // Fraud alert created
        FraudAlert alert = fraudAlertRepository.findByTransactionId(res.getId()).orElseThrow();
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(alert.getRiskScore()).isGreaterThanOrEqualTo(60);
        assertThat(alert.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);

        var hits = fraudAlertHitRepository.findByAlertId(alert.getId());
        assertThat(hits).anyMatch(h -> "R1".equals(h.getRuleCode()));
        assertThat(hits).anyMatch(h -> "R4".equals(h.getRuleCode()));
    }

    @Test
    @DisplayName("Check 3: Critical risk transfers score >= 70 and are BLOCKED immediately with no balance deduction")
    void criticalRiskTransfersBlocked() {
        // Beneficiary added recently (< 24h)
        beneficiaryRepository.save(new Beneficiary(customer, recipientAccount, "Partner"));

        // User has 3 failed logins prior to transfer (triggers R7 = 15 pts)
        customer.setFailedLoginAttempts(3);
        userRepository.save(customer);

        // Transfer 60,000 (R1 = 30 pts, R4 = 30 pts, R7 = 15 pts -> Total 75 pts >= 70)
        TransferRequest req = new TransferRequest(customerAccount.getId(), recipientAccount.getAccountNumber(), new BigDecimal("60000.00"), "Urgent payout");

        TransactionResponse res = transferService.initiateTransfer(req, UUID.randomUUID().toString(), customer.getId(), "127.0.0.1", "REQ-03");

        assertThat(res.getStatus()).isEqualTo(TransactionStatus.BLOCKED);

        Account refreshedSrc = accountRepository.findById(customerAccount.getId()).orElseThrow();
        assertThat(refreshedSrc.getBalance()).isEqualByComparingTo("150000.00");

        FraudAlert alert = fraudAlertRepository.findByTransactionId(res.getId()).orElseThrow();
        assertThat(alert.getRiskScore()).isGreaterThanOrEqualTo(70);
        assertThat(alert.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    @DisplayName("Check 4: RBAC permissions on fraud endpoints - Customer 403, Admin read-only, Admin resolution 403")
    void rbacSecurityOnFraudEndpoints() throws Exception {
        // Customer cannot access fraud alerts
        mockMvc.perform(get("/api/v1/fraud/alerts")
                        .header("Authorization", customerToken))
                .andExpect(status().isForbidden());

        // Admin can list fraud alerts (read-only)
        mockMvc.perform(get("/api/v1/fraud/alerts")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Create a held alert
        beneficiaryRepository.save(new Beneficiary(customer, recipientAccount, "Colleague"));
        TransferRequest req = new TransferRequest(customerAccount.getId(), recipientAccount.getAccountNumber(), new BigDecimal("60000.00"), "Rent");
        TransactionResponse res = transferService.initiateTransfer(req, UUID.randomUUID().toString(), customer.getId(), "127.0.0.1", "REQ-04");
        FraudAlert alert = fraudAlertRepository.findByTransactionId(res.getId()).orElseThrow();

        // Admin attempts to resolve alert -> must fail with 403 Forbidden!
        AlertDecisionRequest decisionReq = new AlertDecisionRequest("RELEASE", "Admin attempting decision");
        mockMvc.perform(post("/api/v1/fraud/alerts/" + alert.getId() + "/decision")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Check 5 & 6: Analyst releases held alert -> moves money, sets RESOLVED_RELEASED; second attempt returns 409")
    void analystReleaseAndConflictOnSecondAttempt() throws Exception {
        beneficiaryRepository.save(new Beneficiary(customer, recipientAccount, "Vendor"));
        TransferRequest req = new TransferRequest(customerAccount.getId(), recipientAccount.getAccountNumber(), new BigDecimal("60000.00"), "Invoices");
        TransactionResponse res = transferService.initiateTransfer(req, UUID.randomUUID().toString(), customer.getId(), "127.0.0.1", "REQ-05");
        FraudAlert alert = fraudAlertRepository.findByTransactionId(res.getId()).orElseThrow();

        // 1. Analyst releases the alert
        AlertNotesRequest notesReq = new AlertNotesRequest("Customer verified over call. Legitimate business invoice.");
        mockMvc.perform(post("/api/v1/fraud/alerts/" + alert.getId() + "/release")
                        .header("Authorization", analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notesReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RESOLVED_RELEASED"));

        // Verify transaction completed and money moved
        Transaction refreshedTx = transactionRepository.findById(res.getId()).orElseThrow();
        assertThat(refreshedTx.getStatus()).isEqualTo(TransactionStatus.APPROVED_SIMULATED);

        Account refreshedSrc = accountRepository.findById(customerAccount.getId()).orElseThrow();
        Account refreshedDst = accountRepository.findById(recipientAccount.getId()).orElseThrow();
        assertThat(refreshedSrc.getBalance()).isEqualByComparingTo("90000.00");
        assertThat(refreshedDst.getBalance()).isEqualByComparingTo("70000.00");

        // 2. Second resolution attempt on already-resolved alert -> must return 409 Conflict
        mockMvc.perform(post("/api/v1/fraud/alerts/" + alert.getId() + "/release")
                        .header("Authorization", analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notesReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    @DisplayName("Check 7: Analyst confirm-block keeps transfer blocked and no money is moved")
    void analystConfirmBlock() throws Exception {
        beneficiaryRepository.save(new Beneficiary(customer, recipientAccount, "Suspicious"));
        TransferRequest req = new TransferRequest(customerAccount.getId(), recipientAccount.getAccountNumber(), new BigDecimal("60000.00"), "Payment");
        TransactionResponse res = transferService.initiateTransfer(req, UUID.randomUUID().toString(), customer.getId(), "127.0.0.1", "REQ-07");
        FraudAlert alert = fraudAlertRepository.findByTransactionId(res.getId()).orElseThrow();

        AlertNotesRequest notesReq = new AlertNotesRequest("Customer confirmed unauthorized account takeover attempt.");
        mockMvc.perform(post("/api/v1/fraud/alerts/" + alert.getId() + "/confirm-block")
                        .header("Authorization", analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notesReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RESOLVED_BLOCKED"));

        Transaction refreshedTx = transactionRepository.findById(res.getId()).orElseThrow();
        assertThat(refreshedTx.getStatus()).isEqualTo(TransactionStatus.BLOCKED);

        Account refreshedSrc = accountRepository.findById(customerAccount.getId()).orElseThrow();
        Account refreshedDst = accountRepository.findById(recipientAccount.getId()).orElseThrow();
        assertThat(refreshedSrc.getBalance()).isEqualByComparingTo("150000.00");
        assertThat(refreshedDst.getBalance()).isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("Check 8: GET /api/v1/fraud/stats returns valid aggregate metrics")
    void fraudStatsEndpointReturnsMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/fraud/stats")
                        .header("Authorization", analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAlerts").isNumber())
                .andExpect(jsonPath("$.data.openAlerts").isNumber())
                .andExpect(jsonPath("$.data.averageRiskScore").isNumber());
    }
}
