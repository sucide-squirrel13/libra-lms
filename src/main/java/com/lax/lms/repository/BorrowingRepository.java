package com.lax.lms.repository;

import com.lax.lms.Borrowing;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {

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