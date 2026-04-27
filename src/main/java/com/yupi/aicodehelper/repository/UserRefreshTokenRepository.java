package com.yupi.aicodehelper.repository;

import com.yupi.aicodehelper.entity.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {

    Optional<UserRefreshToken> findByTokenId(String tokenId);

    Optional<UserRefreshToken> findByTokenIdAndRevokedAtIsNull(String tokenId);

    @Modifying
    @Query("update UserRefreshToken t set t.revokedAt = :revokedAt where t.userId = :userId and t.revokedAt is null")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);
}
