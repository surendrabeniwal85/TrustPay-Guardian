package com.trustpay.guardian.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceId(String referenceId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(Long sourceAccountId, Long destinationAccountId);

    Page<Transaction> findBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(Long sourceAccountId, Long destinationAccountId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId) " +
           "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR t.createdAt <= :endDate) " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findStatementByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t " +
           "WHERE t.sourceAccount.id = :sourceAccountId AND t.status = 'PENDING_REVIEW'")
    BigDecimal sumPendingOutgoingTransfers(@Param("sourceAccountId") Long sourceAccountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t " +
           "WHERE t.sourceAccount.id = :sourceAccountId " +
           "AND t.createdAt >= :since " +
           "AND t.status IN ('APPROVED_SIMULATED', 'PENDING_REVIEW')")
    BigDecimal sumOutgoingTransfersSince(@Param("sourceAccountId") Long sourceAccountId, @Param("since") Instant since);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.sourceAccount.id = :sourceAccountId AND t.createdAt >= :since")
    long countRecentTransfers(@Param("sourceAccountId") Long sourceAccountId, @Param("since") Instant since);

    @Query("SELECT COALESCE(AVG(t.amount), 0.00) FROM Transaction t " +
           "WHERE t.sourceAccount.id = :sourceAccountId AND t.createdAt >= :since AND t.status = 'APPROVED_SIMULATED'")
    BigDecimal findThirtyDayAverage(@Param("sourceAccountId") Long sourceAccountId, @Param("since") Instant since);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.sourceAccount.id = :sourceAccountId AND t.createdAt >= :since AND t.status = 'APPROVED_SIMULATED'")
    long countThirtyDayTransfers(@Param("sourceAccountId") Long sourceAccountId, @Param("since") Instant since);

    long countByStatus(TransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t WHERE t.status = 'APPROVED_SIMULATED'")
    BigDecimal sumCompletedVolume();

    @Query("SELECT t FROM Transaction t WHERE (t.sourceAccount.user.id = :userId OR t.destinationAccount.user.id = :userId) " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR t.createdAt <= :endDate) " +
           "AND (:minAmount IS NULL OR t.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR t.amount <= :maxAmount) " +
           "AND (:search IS NULL OR LOWER(t.referenceId) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findFilteredTransactionsForUser(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:type IS NULL OR t.type = :type) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR t.createdAt <= :endDate) " +
           "AND (:minAmount IS NULL OR t.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR t.amount <= :maxAmount) " +
           "AND (:search IS NULL OR LOWER(t.referenceId) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findAllFilteredTransactions(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT t.createdAt FROM Transaction t WHERE t.createdAt >= :since")
    List<Instant> findCreatedAtSince(@Param("since") Instant since);
}

