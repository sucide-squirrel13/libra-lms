package com.lax.lms.repository;

import com.lax.lms.Borrowing;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Borrowing b WHERE b.id = :id")
    Optional<Borrowing> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"book", "user"})
    List<Borrowing> findByUserIdOrderByBorrowDateDesc(Long userId);

    @EntityGraph(attributePaths = {"book", "user"})
    List<Borrowing> findByStatusOrderByDueDateAsc(String status);

    @EntityGraph(attributePaths = {"book", "user"})
    List<Borrowing> findAllByOrderByBorrowDateDesc();

    boolean existsByUserIdAndBookIdAndStatus(
            Long userId,
            Long bookId,
            String status
    );
    long countByStatus(String status);

    long countByStatusAndDueDateBefore(
            String status,
            java.time.LocalDate date);
}