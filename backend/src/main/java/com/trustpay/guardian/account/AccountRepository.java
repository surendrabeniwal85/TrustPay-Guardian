package com.trustpay.guardian.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    @Query("SELECT a.id FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Long> findIdByAccountNumber(@Param("accountNumber") String accountNumber);

    List<Account> findByUserId(Long userId);

    long countByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(a.balance), 0.00) FROM Account a")
    BigDecimal sumTotalBalances();

    @Query("""
        SELECT a FROM Account a
        WHERE (:status IS NULL OR a.status = :status)
          AND (:search IS NULL OR LOWER(a.accountNumber) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(a.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Account> findFilteredAccounts(
            @Param("status") AccountStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
