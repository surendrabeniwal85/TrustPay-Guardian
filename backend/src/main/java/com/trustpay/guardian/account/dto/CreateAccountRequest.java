package com.trustpay.guardian.account.dto;

import com.trustpay.guardian.account.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for creating an additional bank account")
public class CreateAccountRequest {

    @Schema(description = "Account type (SAVINGS, CURRENT, SALARY)", example = "SAVINGS")
    private AccountType accountType = AccountType.SAVINGS;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(AccountType accountType) {
        this.accountType = (accountType != null) ? accountType : AccountType.SAVINGS;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = (accountType != null) ? accountType : AccountType.SAVINGS;
    }
}
