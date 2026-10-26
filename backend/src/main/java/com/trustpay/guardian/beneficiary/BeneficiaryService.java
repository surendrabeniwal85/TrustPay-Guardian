package com.trustpay.guardian.beneficiary;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.beneficiary.dto.BeneficiaryLookupResponse;
import com.trustpay.guardian.beneficiary.dto.BeneficiaryRequest;
import com.trustpay.guardian.beneficiary.dto.BeneficiaryResponse;
import com.trustpay.guardian.common.exception.BusinessRuleException;
import com.trustpay.guardian.common.exception.ConflictException;
import com.trustpay.guardian.common.exception.NotFoundException;
import com.trustpay.guardian.common.util.MaskingUtils;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public BeneficiaryService(
            BeneficiaryRepository beneficiaryRepository,
            AccountRepository accountRepository,
            UserRepository userRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public BeneficiaryLookupResponse lookup(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new BusinessRuleException("Account number is required for lookup");
        }
        Account account = accountRepository.findByAccountNumber(accountNumber.trim())
                .orElseThrow(() -> new NotFoundException("Account not found with account number: " + accountNumber));

        String maskedHolder = MaskingUtils.maskName(account.getUser().getFullName());
        return new BeneficiaryLookupResponse(account.getAccountNumber(), maskedHolder);
    }

    @Transactional
    public BeneficiaryResponse addBeneficiary(BeneficiaryRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        Account recipientAccount = accountRepository.findByAccountNumber(request.getRecipientAccountNumber().trim())
                .orElseThrow(() -> new NotFoundException("Recipient account not found: " + request.getRecipientAccountNumber()));

        // Cannot add own account as beneficiary
        if (recipientAccount.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("Cannot add your own account as a beneficiary.");
        }

        // Check compound uniqueness
        if (beneficiaryRepository.findByUserIdAndRecipientAccountId(userId, recipientAccount.getId()).isPresent()) {
            throw new ConflictException("This beneficiary account is already registered in your address book.");
        }

        Beneficiary beneficiary = new Beneficiary(user, recipientAccount, request.getNickname().trim());
        Beneficiary saved = beneficiaryRepository.save(beneficiary);

        return BeneficiaryResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getUserBeneficiaries(Long userId) {
        return beneficiaryRepository.findByUserId(userId).stream()
                .map(BeneficiaryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteBeneficiary(Long beneficiaryId, Long userId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new NotFoundException("Beneficiary not found with id: " + beneficiaryId));

        // IDOR protection: return 404 (not 403) to prevent ID enumeration
        if (!beneficiary.getUser().getId().equals(userId)) {
            throw new NotFoundException("Beneficiary not found with id: " + beneficiaryId);
        }

        beneficiaryRepository.delete(beneficiary);
    }
}
