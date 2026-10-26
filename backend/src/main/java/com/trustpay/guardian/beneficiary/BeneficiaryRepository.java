package com.trustpay.guardian.beneficiary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Beneficiary b " +
           "JOIN FETCH b.recipientAccount ra " +
           "JOIN FETCH ra.user u " +
           "WHERE b.user.id = :userId")
    List<Beneficiary> findByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    Optional<Beneficiary> findByUserIdAndRecipientAccountId(Long userId, Long recipientAccountId);

    boolean existsByUserIdAndRecipientAccountId(Long userId, Long recipientAccountId);
}
