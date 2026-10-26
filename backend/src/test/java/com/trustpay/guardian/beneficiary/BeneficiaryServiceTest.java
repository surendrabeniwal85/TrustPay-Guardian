package com.trustpay.guardian.beneficiary;

import com.trustpay.guardian.account.Account;
import com.trustpay.guardian.account.AccountRepository;
import com.trustpay.guardian.account.AccountType;
import com.trustpay.guardian.beneficiary.dto.BeneficiaryLookupResponse;
import com.trustpay.guardian.beneficiary.dto.BeneficiaryRequest;
import com.trustpay.guardian.beneficiary.dto.BeneficiaryResponse;
import com.trustpay.guardian.common.exception.BusinessRuleException;
import com.trustpay.guardian.common.exception.ConflictException;
import com.trustpay.guardian.common.exception.NotFoundException;
import com.trustpay.guardian.user.Role;
import com.trustpay.guardian.user.User;
import com.trustpay.guardian.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    private BeneficiaryService beneficiaryService;

    @BeforeEach
    void setUp() {
        beneficiaryService = new BeneficiaryService(beneficiaryRepository, accountRepository, userRepository);
    }

    @Test
    @DisplayName("Lookup returns masked account holder name to protect privacy")
    void lookupReturnsMaskedAccountHolderName() {
        User recipientUser = new User("Jane Elizabeth Smith", "jane@example.com", "9876543211", "hash", Role.CUSTOMER);
        Account recipientAccount = new Account(recipientUser, "TPG1000000002", AccountType.SAVINGS, BigDecimal.TEN, BigDecimal.TEN);

        when(accountRepository.findByAccountNumber("TPG1000000002")).thenReturn(Optional.of(recipientAccount));

        BeneficiaryLookupResponse lookup = beneficiaryService.lookup("TPG1000000002");

        assertThat(lookup.accountNumber()).isEqualTo("TPG1000000002");
        assertThat(lookup.maskedAccountHolderName()).isEqualTo("J*** E******** S****");
    }

    @Test
    @DisplayName("Lookup of non-existent account returns 404 NotFoundException")
    void lookupNonExistentAccountReturns404() {
        when(accountRepository.findByAccountNumber("TPG0000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> beneficiaryService.lookup("TPG0000000000"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Account not found with account number: TPG0000000000");
    }

    @Test
    @DisplayName("Adding own account as beneficiary is rejected with BusinessRuleException (422)")
    void addingOwnAccountAsBeneficiaryRejected() {
        User user = new User("John Doe", "john@example.com", "9876543210", "hash", Role.CUSTOMER);
        user.setId(1L);

        Account ownAccount = new Account(user, "TPG1000000001", AccountType.SAVINGS, BigDecimal.TEN, BigDecimal.TEN);
        ownAccount.setId(10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber("TPG1000000001")).thenReturn(Optional.of(ownAccount));

        BeneficiaryRequest request = new BeneficiaryRequest("TPG1000000001", "My Other Account");

        assertThatThrownBy(() -> beneficiaryService.addBeneficiary(request, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot add your own account as a beneficiary");
    }

    @Test
    @DisplayName("Adding a duplicate beneficiary is rejected with ConflictException (409)")
    void addingDuplicateBeneficiaryRejected() {
        User user = new User("John", "john@example.com", "9876543210", "hash", Role.CUSTOMER);
        user.setId(1L);

        User recipient = new User("Jane", "jane@example.com", "9876543211", "hash", Role.CUSTOMER);
        recipient.setId(2L);

        Account recipientAccount = new Account(recipient, "TPG1000000002", AccountType.SAVINGS, BigDecimal.TEN, BigDecimal.TEN);
        recipientAccount.setId(20L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber("TPG1000000002")).thenReturn(Optional.of(recipientAccount));
        when(beneficiaryRepository.findByUserIdAndRecipientAccountId(1L, 20L)).thenReturn(Optional.of(new Beneficiary()));

        BeneficiaryRequest request = new BeneficiaryRequest("TPG1000000002", "Jane Smith");

        assertThatThrownBy(() -> beneficiaryService.addBeneficiary(request, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Deleting another user's beneficiary returns 404 NotFoundException (IDOR Protection)")
    void deleteOtherUserBeneficiaryReturns404() {
        User userB = new User("User B", "b@example.com", "9876543211", "hash", Role.CUSTOMER);
        userB.setId(2L);

        Beneficiary beneficiaryB = new Beneficiary(userB, new Account(), "B's contact");
        beneficiaryB.setId(99L);

        when(beneficiaryRepository.findById(99L)).thenReturn(Optional.of(beneficiaryB));

        // User A (id: 1L) attempts to delete User B's beneficiary (id: 99L)
        assertThatThrownBy(() -> beneficiaryService.deleteBeneficiary(99L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Beneficiary not found with id: 99");

        verify(beneficiaryRepository, never()).delete(any());
    }
}
