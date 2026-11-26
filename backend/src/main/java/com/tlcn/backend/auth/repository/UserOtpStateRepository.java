package com.tlcn.backend.auth.repository;

import com.tlcn.backend.auth.entity.UserOtpState;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserOtpStateRepository extends JpaRepository<UserOtpState, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserOtpState s where s.userId = :userId")
    Optional<UserOtpState> findForUpdate(@Param("userId") Long userId);

    @Query("select s.userId from UserOtpState s where s.resetTokenHash = :hash")
    Optional<Long> findUserIdByResetTokenHash(@Param("hash") String hash);
}
