package com.trustpay.guardian.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountStatus;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.account.dto.DepositRequest;
import com.trustpay.guardian.security.JwtService;
import com.trustpay.guardian.transaction.dto.TransferRequest;
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
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class TransactionControllerMockMvcTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("POST /transactions/transfer succeeds, debits sender, credits recipient, and respects idempotency")
    void transferLifecycleAndIdempotencyReplay() throws Exception {
        User sender = userRepository.save(new User("Alice Tx", "alice.tx@example.com", "9811111101", "hash", Role.CUSTOMER));
        User recipient = userRepository.save(new User("Bob Tx", "bob.tx@example.com", "9811111102", "hash", Role.CUSTOMER));

        Account src = accountRepository.save(new Account(sender, "TPG_TX_A1", AccountType.SAVINGS, new BigDecimal("20000.00"), new BigDecimal("100000.00")));
        Account dst = accountRepository.save(new Account(recipient, "TPG_TX_B1", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("100000.00")));

        String senderToken = jwtService.generateAccessToken(com.trustpay.guardian.security.UserPrincipal.create(sender));
        String idempotencyKey = UUID.randomUUID().toString();

        TransferRequest req = new TransferRequest(src.getId(), dst.getAccountNumber(), new BigDecimal("2500.00"), "Invoice 101");

        // First attempt: succeeds with 200 OK
        String firstResponse = mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + senderToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("APPROVED_SIMULATED")))
                .andExpect(jsonPath("$.data.amount", is(2500.00)))
                .andReturn().getResponse().getContentAsString();

        // Check balances
        Account updatedSrc = accountRepository.findById(src.getId()).orElseThrow();
        Account updatedDst = accountRepository.findById(dst.getId()).orElseThrow();
        assertThat(updatedSrc.getBalance()).isEqualByComparingTo("17500.00");
        assertThat(updatedDst.getBalance()).isEqualByComparingTo("7500.00");

        // Second attempt with identical Idempotency-Key: returns original result without double debit
        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + senderToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("APPROVED_SIMULATED")))
                .andExpect(jsonPath("$.data.amount", is(2500.00)));

        // Balances remain untouched after replay
        Account recheckedSrc = accountRepository.findById(src.getId()).orElseThrow();
        Account recheckedDst = accountRepository.findById(dst.getId()).orElseThrow();
        assertThat(recheckedSrc.getBalance()).isEqualByComparingTo("17500.00");
        assertThat(recheckedDst.getBalance()).isEqualByComparingTo("7500.00");
    }

    @Test
    @DisplayName("POST /transactions/transfer fails with 422 if Idempotency-Key is missing or invalid")
    void transferRejectsMissingOrInvalidIdempotencyKey() throws Exception {
        User user = userRepository.save(new User("Key User", "key.user@example.com", "9811111103", "hash", Role.CUSTOMER));
        Account src = accountRepository.save(new Account(user, "TPG_TX_KEY", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00")));
        String token = jwtService.generateAccessToken(com.trustpay.guardian.security.UserPrincipal.create(user));
        TransferRequest req = new TransferRequest(src.getId(), "TPG9999999999", new BigDecimal("100.00"), "No key");

        // Missing Idempotency-Key header -> 422
        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("Idempotency-Key header is required")));

        // Invalid UUID format -> 422
        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "not-a-valid-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("must be a valid UUID")));
    }

    @Test
    @DisplayName("Transfer to a FROZEN account fails with 422 and leaves balances unchanged")
    void transferToFrozenAccountFailsWith422() throws Exception {
        User sender = userRepository.save(new User("Sender Freeze", "sender.frz@example.com", "9811111104", "hash", Role.CUSTOMER));
        User frozenUser = userRepository.save(new User("Frozen User", "frozen.user@example.com", "9811111105", "hash", Role.CUSTOMER));

        Account src = accountRepository.save(new Account(sender, "TPG_TX_ACT", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00")));
        Account frozenDst = new Account(frozenUser, "TPG_TX_FRZ", AccountType.SAVINGS, new BigDecimal("5000.00"), new BigDecimal("100000.00"));
        frozenDst.setStatus(AccountStatus.FROZEN);
        frozenDst = accountRepository.save(frozenDst);

        String token = jwtService.generateAccessToken(com.trustpay.guardian.security.UserPrincipal.create(sender));
        TransferRequest req = new TransferRequest(src.getId(), frozenDst.getAccountNumber(), new BigDecimal("1000.00"), "Transfer to frozen");

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("FROZEN")));

        // Balances must remain strictly unchanged
        assertThat(accountRepository.findById(src.getId()).orElseThrow().getBalance()).isEqualByComparingTo("10000.00");
        assertThat(accountRepository.findById(frozenDst.getId()).orElseThrow().getBalance()).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("Transfer exceeding available balance fails with 422")
    void transferExceedingBalanceFailsWith422() throws Exception {
        User sender = userRepository.save(new User("Sender Low", "sender.low@example.com", "9811111106", "hash", Role.CUSTOMER));
        User recipient = userRepository.save(new User("Recipient High", "recip.high@example.com", "9811111107", "hash", Role.CUSTOMER));

        Account src = accountRepository.save(new Account(sender, "TPG_TX_LOW", AccountType.SAVINGS, new BigDecimal("500.00"), new BigDecimal("100000.00")));
        Account dst = accountRepository.save(new Account(recipient, "TPG_TX_DST1", AccountType.SAVINGS, new BigDecimal("1000.00"), new BigDecimal("100000.00")));

        String token = jwtService.generateAccessToken(com.trustpay.guardian.security.UserPrincipal.create(sender));
        TransferRequest req = new TransferRequest(src.getId(), dst.getAccountNumber(), new BigDecimal("600.00"), "Overdraft");

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("Insufficient funds")));
    }

    @Test
    @DisplayName("Transfer with invalid decimal scale (>2) fails with 422")
    void transferWithInvalidScaleFailsWith422() throws Exception {
        User sender = userRepository.save(new User("Scale User", "scale.user@example.com", "9811111108", "hash", Role.CUSTOMER));
        Account src = accountRepository.save(new Account(sender, "TPG_TX_SCALE", AccountType.SAVINGS, new BigDecimal("1000.00"), new BigDecimal("100000.00")));
        String token = jwtService.generateAccessToken(com.trustpay.guardian.security.UserPrincipal.create(sender));

        TransferRequest req = new TransferRequest(src.getId(), "TPG_TX_SCALE", new BigDecimal("10.555"), "3 decimals");

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("decimal places")));
    }

    @Test
    @DisplayName("POST /transactions/deposit deposits demo money and updates balance")
    void depositLifecycleSucceeds() throws Exception {
        User user = userRepository.save(new User("Deposit User", "deposit.user@example.com", "9811111109", "hash", Role.CUSTOMER));
        Account acc = accountRepository.save(new Account(user, "TPG_TX_DEP", AccountType.SAVINGS, new BigDecimal("1000.00"), new BigDecimal("100000.00")));
        String token = jwtService.generateAccessToken(com.trustpay.guardian.security.UserPrincipal.create(user));

        DepositRequest req = new DepositRequest(acc.getId(), new BigDecimal("5000.00"));

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.type", is("DEPOSIT")))
                .andExpect(jsonPath("$.data.amount", is(5000.00)));

        Account updated = accountRepository.findById(acc.getId()).orElseThrow();
        assertThat(updated.getBalance()).isEqualByComparingTo("6000.00");
    }

    @Test
    @DisplayName("GET /transactions and GET /transactions/{reference} IDOR enforcement")
    void listAndReferenceLookupIdorEnforcement() throws Exception {
        User alice = userRepository.save(new User("Alice Query", "alice.query@example.com", "9811111110", "hash", Role.CUSTOMER));
        User eve = userRepository.save(new User("Eve Query", "eve.query@example.com", "9811111111", "hash", Role.CUSTOMER));

        Account aliceAcc = accountRepository.save(new Account(alice, "TPG_ALICE_Q", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00")));
        Account eveAcc = accountRepository.save(new Account(eve, "TPG_EVE_Q", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("100000.00")));

        String aliceToken = jwtService.generateAccessToken(com.trustpay.guardian.security.UserPrincipal.create(alice));
        String eveToken = jwtService.generateAccessToken(com.trustpay.guardian.security.UserPrincipal.create(eve));

        // Perform transfer from Alice to Eve
        TransferRequest req = new TransferRequest(aliceAcc.getId(), eveAcc.getAccountNumber(), new BigDecimal("1000.00"), "Query test");
        String jsonRes = mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + aliceToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refId = objectMapper.readTree(jsonRes).path("data").path("referenceId").asText();

        // 1. Alice can list transactions and sees the transfer
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].referenceId", is(refId)));

        // 2. Alice (sender) can query transaction by reference
        mockMvc.perform(get("/api/v1/transactions/" + refId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referenceId", is(refId)));

        // 3. Eve (receiver) can query transaction by reference
        mockMvc.perform(get("/api/v1/transactions/" + refId)
                        .header("Authorization", "Bearer " + eveToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referenceId", is(refId)));

        // 4. Mallory (unrelated user) querying refId gets 404 (IDOR safe)
        User mallory = userRepository.save(new User("Mallory Query", "mallory.query@example.com", "9811111112", "hash", Role.CUSTOMER));
        String malloryToken = jwtService.generateAccessToken(com.trustpay.guardian.security.UserPrincipal.create(mallory));

        mockMvc.perform(get("/api/v1/transactions/" + refId)
                        .header("Authorization", "Bearer " + malloryToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/transfers initiates transfer and GET /api/v1/transfers/account/{id} returns account transactions")
    void testDirectTransfersEndpoints() throws Exception {
        User user1 = userRepository.save(new User("T Cust 1", "tcust1@example.com", "9811111191", "hash", Role.CUSTOMER));
        User user2 = userRepository.save(new User("T Cust 2", "tcust2@example.com", "9811111192", "hash", Role.CUSTOMER));

        Account acc1 = accountRepository.save(new Account(user1, "TPG_T1_001", AccountType.SAVINGS, new BigDecimal("10000.00"), new BigDecimal("50000.00")));
        Account acc2 = accountRepository.save(new Account(user2, "TPG_T2_001", AccountType.SAVINGS, new BigDecimal("2000.00"), new BigDecimal("50000.00")));

        String token1 = jwtService.generateAccessToken(com.trustpay.guardian.security.UserPrincipal.create(user1));

        TransferRequest req = new TransferRequest(acc1.getId(), acc2.getAccountNumber(), new BigDecimal("500.00"), "Transfer test");

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token1)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("APPROVED_SIMULATED")));

        // Query transactions for account1
        mockMvc.perform(get("/api/v1/transfers/account/" + acc1.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isArray());
    }
}
