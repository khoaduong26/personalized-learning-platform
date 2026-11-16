package com.tlcn.backend.auth.repository;

import com.tlcn.backend.auth.entity.LoginSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginSessionRepository extends JpaRepository<LoginSession, String> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from LoginSession s where s.userId = :userId")
    int deleteByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from LoginSession s where s.expiresAt <= :now")
    int deleteExpired(@org.springframework.data.repository.query.Param("now") java.time.Instant now);
}
