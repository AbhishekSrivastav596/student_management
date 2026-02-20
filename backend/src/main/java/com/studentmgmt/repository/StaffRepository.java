package com.studentmgmt.repository;

import com.studentmgmt.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    @Query("SELECT s FROM Staff s WHERE " +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.department) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Staff> search(@Param("search") String search, Pageable pageable);

    Page<Staff> findByActive(boolean active, Pageable pageable);

    @Query("SELECT s FROM Staff s WHERE s.active = :active AND (" +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.department) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Staff> searchByActive(@Param("search") String search, @Param("active") boolean active, Pageable pageable);

    List<Staff> findAllByIdIn(List<Long> ids);
}
