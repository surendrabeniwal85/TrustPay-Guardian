package com.trustpay.guardian.admin.dto;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountStatus;
import com.trustpay.guardian.account.AccountType;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountManagementDto(
        Long id,
        String accountNumber,
        String maskedAccountNumber,
        Long userId,
        String holderName,
        String holderEmail,
        AccountType accountType,
        BigDecimal balance,
        BigDecimal dailyLimit,
        AccountStatus status,
        Instant createdAt
) {
    public static AccountManagementDto fromEntity(Account account) {
        return new AccountManagementDto(
                account.getId(),
                account.getAccountNumber(),
                account.getMaskedAccountNumber(),
                account.getUser() != null ? account.getUser().getId() : null,
                account.getUser() != null ? account.getUser().getFullName() : null,
                account.getUser() != null ? account.getUser().getEmail() : null,
                account.getAccountType(),
                account.getBalance(),
                account.getDailyLimit(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
