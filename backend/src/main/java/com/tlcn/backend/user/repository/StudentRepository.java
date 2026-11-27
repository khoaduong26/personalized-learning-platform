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
    @Query("select distinct s from Student s left join s.additionalClassrooms extra "
            + "where s.classroom.id = :classroomId or extra.id = :classroomId order by s.fullName asc")
    List<Student> findMembersOfClassroom(@Param("classroomId") Long classroomId);

    @Query("select count(distinct s.id) from Student s left join s.additionalClassrooms extra "
            + "where s.classroom.id = :classroomId or extra.id = :classroomId")
    long countMembersOfClassroom(@Param("classroomId") Long classroomId);

    Optional<Student> findByEmailIgnoreCaseAndStatus(String email, UserStatus status);

    @Query("select case when count(s) > 0 then true else false end from Student s "
            + "where s.id = :studentId and (s.classroom.id = :classroomId or exists "
            + "(select extra.id from Student member join member.additionalClassrooms extra "
            + "where member.id = :studentId and extra.id = :classroomId))")
    boolean isMemberOfClassroom(@Param("studentId") Long studentId, @Param("classroomId") Long classroomId);

    @Query("select case when count(s) > 0 then true else false end from Student s "
            + "where s.id = :studentId and (s.classroom.teacher.id = :teacherId or exists "
            + "(select extra.id from Student member join member.additionalClassrooms extra "
            + "where member.id = :studentId and extra.teacher.id = :teacherId))")
    boolean hasClassroomOwnedBy(@Param("studentId") Long studentId, @Param("teacherId") Long teacherId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Student s where s.id = :id")
    Optional<Student> findByIdForUpdate(@Param("id") Long id);
}
