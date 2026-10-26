package com.trustpay.guardian.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustpay.guardian.account.dto.CreateAccountRequest;
import com.trustpay.guardian.auth.dto.LoginRequest;
import com.trustpay.guardian.auth.dto.RegisterRequest;
import com.trustpay.guardian.beneficiary.dto.BeneficiaryRequest;
import com.trustpay.guardian.common.web.RequestIdFilter;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class AccountAndBeneficiaryMockMvcTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestIdFilter requestIdFilter;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .addFilters(requestIdFilter)
                .build();
    }

    private String registerAndGetToken(String fullName, String email, String phone, String password) throws Exception {
        RegisterRequest registerReq = new RegisterRequest(fullName, email, phone, password);
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Complete Account & Beneficiary Journey with IDOR and Limit Enforcements")
    void accountAndBeneficiaryJourneyWithIdorEnforcement() throws Exception {
        // 1. Register User A
        String tokenA = registerAndGetToken(
                "Customer Alpha",
                "alpha@example.com",
                "9876540001",
                "AlphaSecure@123"
        );

        // 2. User A sees exactly one default account, with masked account number
        String accountsJsonA = mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].accountNumber", startsWith("TPG")))
                .andExpect(jsonPath("$.data[0].maskedAccountNumber", matchesPattern("^TPG\\*+[0-9]{4}$")))
                .andReturn().getResponse().getContentAsString();

        long accountIdA = objectMapper.readTree(accountsJsonA).path("data").get(0).path("id").asLong();
        String accountNumberA = objectMapper.readTree(accountsJsonA).path("data").get(0).path("accountNumber").asText();

        // 3. User A opens 2nd account
        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAccountRequest(AccountType.CURRENT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accountType").value("CURRENT"));

        // 4. User A opens 3rd account
        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAccountRequest(AccountType.SAVINGS))))
                .andExpect(status().isCreated());

        // Verify User A now has 3 accounts
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));

        // 5. User A tries to open a 4th account -> 422 Unprocessable Entity
        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAccountRequest(AccountType.SAVINGS))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("Maximum limit of 3 accounts reached for this customer."));

        // 6. Register User B
        String tokenB = registerAndGetToken(
                "Customer Beta",
                "beta@example.com",
                "9876540002",
                "BetaSecure@123"
        );

        String accountsJsonB = mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andReturn().getResponse().getContentAsString();

        long accountIdB = objectMapper.readTree(accountsJsonB).path("data").get(0).path("id").asLong();
        String accountNumberB = objectMapper.readTree(accountsJsonB).path("data").get(0).path("accountNumber").asText();

        // 7. EXPLICIT IDOR CHECK: User A requesting User B's account id returns 404 Not Found
        mockMvc.perform(get("/api/v1/accounts/" + accountIdB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // 8. EXPLICIT IDOR CHECK: User A requesting User B's statement returns 404 Not Found
        mockMvc.perform(get("/api/v1/accounts/" + accountIdB + "/statement")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // 9. User A requests own statement -> 200 OK
        mockMvc.perform(get("/api/v1/accounts/" + accountIdA + "/statement")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        // 10. Beneficiary Lookup: User A looks up User B's account number -> returns masked holder name
        mockMvc.perform(get("/api/v1/beneficiaries/lookup")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("accountNumber", accountNumberB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountNumber").value(accountNumberB))
                .andExpect(jsonPath("$.data.maskedAccountHolderName").value("C******* B***"));

        // 11. User A tries to add own account as beneficiary -> 422 Unprocessable Entity
        BeneficiaryRequest selfBeneficiary = new BeneficiaryRequest(accountNumberA, "My Own Account");
        mockMvc.perform(post("/api/v1/beneficiaries")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfBeneficiary)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));

        // 12. User A adds User B as beneficiary -> 201 Created
        BeneficiaryRequest validBeneficiary = new BeneficiaryRequest(accountNumberB, "Beta Partner");
        String addBeneficiaryResponse = mockMvc.perform(post("/api/v1/beneficiaries")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBeneficiary)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nickname").value("Beta Partner"))
                .andReturn().getResponse().getContentAsString();

        long beneficiaryId = objectMapper.readTree(addBeneficiaryResponse).path("data").path("id").asLong();

        // 13. User A tries to add User B again (duplicate) -> 409 Conflict
        mockMvc.perform(post("/api/v1/beneficiaries")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBeneficiary)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // 14. EXPLICIT IDOR CHECK: User B tries to delete User A's beneficiary -> 404 Not Found
        mockMvc.perform(delete("/api/v1/beneficiaries/" + beneficiaryId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // 15. User A deletes own beneficiary -> 200 OK
        mockMvc.perform(delete("/api/v1/beneficiaries/" + beneficiaryId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Beneficiary removed successfully"));
    }
}
