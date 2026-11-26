package com.tlcn.backend.auth.repository;

import com.tlcn.backend.auth.entity.UserGoogleIdentity;
import com.tlcn.backend.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserGoogleIdentityRepository extends JpaRepository<UserGoogleIdentity, Long> {
    boolean existsBySubject(String subject);

    @Query("select g.user from UserGoogleIdentity g where g.subject = :subject")
    Optional<User> findUserBySubject(String subject);
}
