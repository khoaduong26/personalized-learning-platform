package com.tlcn.backend.user.repository;

import com.tlcn.backend.user.entity.UserAccountDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountDetailsRepository extends JpaRepository<UserAccountDetails, Long> {}
