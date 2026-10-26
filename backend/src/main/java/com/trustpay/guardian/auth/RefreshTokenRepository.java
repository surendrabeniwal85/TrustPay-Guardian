package com.trustpay.guardian.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserId(Long userId);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId);

    void deleteByUserId(Long userId);
}
