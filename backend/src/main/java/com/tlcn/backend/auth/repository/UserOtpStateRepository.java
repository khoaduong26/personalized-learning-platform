package com.tlcn.backend.auth.repository;

import com.tlcn.backend.auth.entity.UserOtpState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOtpStateRepository extends JpaRepository<UserOtpState, Long> {}
