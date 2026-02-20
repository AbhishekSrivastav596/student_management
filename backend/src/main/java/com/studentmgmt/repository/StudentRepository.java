package com.studentmgmt.repository;

import com.studentmgmt.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Student> search(@Param("search") String search, Pageable pageable);

    Page<Student> findByActive(boolean active, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE s.active = :active AND (" +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Student> searchByActive(@Param("search") String search, @Param("active") boolean active, Pageable pageable);

    List<Student> findAllByIdIn(List<Long> ids);

    long countByActive(boolean active);
}
