package com.lax.lms.repository;

import com.lax.lms.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    // Lock a book row while processing a borrowing transaction.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :bookId")
    Optional<Book> findByIdForUpdate(@Param("bookId") Long bookId);

    // Search books by title or author.
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(
            String title,
            String author
    );

    // Retrieve distinct categories for the catalogue.
    @Query("SELECT DISTINCT b.category FROM Book b ORDER BY b.category")
    List<String> findDistinctCategories();

    // Search the catalogue by keyword and category.
    @Query("""
            SELECT b FROM Book b
            WHERE (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (
                :category IS NULL OR :category = ''
                OR b.category = :category
            )
            ORDER BY b.title ASC
            """)
    List<Book> searchCatalogue(
            @Param("keyword") String keyword,
            @Param("category") String category
    );
    long count();

    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM Book b")
    long countTotalCopies();

    @Query("SELECT COALESCE(SUM(b.availableCopies), 0) FROM Book b")
    long countAvailableCopies();
}