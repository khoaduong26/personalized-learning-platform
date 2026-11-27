package com.tlcn.backend.academic.repository;

import com.tlcn.backend.academic.entity.Subject;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    boolean existsByNameIgnoreCaseAndOwnerIsNull(String name);

    boolean existsByNameIgnoreCaseAndOwner_Id(String name, Long ownerId);

    java.util.List<Subject> findAllByOrderByNameAsc();

    @Query("select s from Subject s where s.owner is null or s.owner.id = :ownerId order by s.name asc")
    java.util.List<Subject> findVisibleForOwner(@Param("ownerId") Long ownerId);

    java.util.List<Subject> findByOwnerIsNullOrderByNameAsc();
}
