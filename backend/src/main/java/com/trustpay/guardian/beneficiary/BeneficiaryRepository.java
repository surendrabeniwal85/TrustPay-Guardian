package com.trustpay.guardian.beneficiary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByUserId(Long userId);

    Optional<Beneficiary> findByUserIdAndRecipientAccountId(Long userId, Long recipientAccountId);

    boolean existsByUserIdAndRecipientAccountId(Long userId, Long recipientAccountId);
}
