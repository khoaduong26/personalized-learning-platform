package com.tlcn.backend.user.repository;
import com.tlcn.backend.user.entity.Student;
import com.tlcn.backend.user.entity.UserStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByClassroomIdOrderByFullNameAsc(Long classroomId);
    long countByClassroomId(Long classroomId);
    Optional<Student> findByEmailIgnoreCaseAndClassroomIsNullAndStatus(String email, UserStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Student s where s.id = :id")
    Optional<Student> findByIdForUpdate(@Param("id") Long id);
}
