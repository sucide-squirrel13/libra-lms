package com.lax.lms;

import com.lax.lms.repository.BookRepository;
import com.lax.lms.repository.BorrowingRepository;
import com.lax.lms.repository.LibraryUserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class BorrowingService {

    private static final int LOAN_PERIOD_DAYS = 14;

    private final BookRepository bookRepository;
    private final BorrowingRepository borrowingRepository;
    private final LibraryUserRepository userRepository;
    private final AuditService auditService;

    public BorrowingService(
            BookRepository bookRepository,
            BorrowingRepository borrowingRepository,
            LibraryUserRepository userRepository,
            AuditService auditService) {

        this.bookRepository = bookRepository;
        this.borrowingRepository = borrowingRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void borrowBook(Long userId, Long bookId) {

        LibraryUser user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        try {
            // Lock the book row until this transaction completes.
            Book book = bookRepository.findByIdForUpdate(bookId)
                    .orElseThrow(() ->
                            new IllegalArgumentException("Book not found."));

            if (book.getAvailableCopies() <= 0) {
                throw new IllegalStateException(
                        "This book is currently unavailable.");
            }

            if (borrowingRepository.existsByUserIdAndBookIdAndStatus(
                    userId, bookId, "BORROWED")) {

                throw new IllegalStateException(
                        "You have already borrowed this book.");
            }

            LocalDate borrowDate = LocalDate.now();

            Borrowing borrowing = new Borrowing();
            borrowing.setUser(user);
            borrowing.setBook(book);
            borrowing.setBorrowDate(borrowDate);
            borrowing.setDueDate(
                    borrowDate.plusDays(LOAN_PERIOD_DAYS));
            borrowing.setStatus("BORROWED");

            book.setAvailableCopies(book.getAvailableCopies() - 1);

            bookRepository.save(book);
            borrowingRepository.save(borrowing);

            auditService.record(
                    user.getEmail(),
                    "BOOK_BORROW",
                    "Borrowed book ID: " + bookId
                            + "; due date: " + borrowing.getDueDate(),
                    "SUCCESS");

        } catch (IllegalArgumentException | IllegalStateException exception) {

            auditService.record(
                    user.getEmail(),
                    "BOOK_BORROW",
                    "Book ID: " + bookId
                            + "; reason: " + exception.getMessage(),
                    "FAILURE");

            throw exception;
        }
    }

    @Transactional
    public void returnBook(Long borrowingId, Long userId) {

        LibraryUser user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        try {
            Borrowing borrowing = borrowingRepository.findById(borrowingId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Borrowing record not found."));

            if (!borrowing.getUser().getId().equals(userId)) {
                throw new IllegalStateException(
                        "You cannot return another user's borrowing.");
            }

            if (!"BORROWED".equals(borrowing.getStatus())) {
                throw new IllegalStateException(
                        "This book has already been returned.");
            }

            // Lock the associated book before changing availability.
            Book book = bookRepository.findByIdForUpdate(
                            borrowing.getBook().getId())
                    .orElseThrow(() ->
                            new IllegalStateException("Book not found."));

            if (book.getAvailableCopies() < 0
                    || book.getAvailableCopies() >= book.getQuantity()) {

                throw new IllegalStateException(
                        "Book availability is inconsistent. Contact the librarian.");
            }

            borrowing.setReturnDate(LocalDate.now());
            borrowing.setStatus("RETURNED");

            book.setAvailableCopies(book.getAvailableCopies() + 1);

            borrowingRepository.save(borrowing);
            bookRepository.save(book);

            auditService.record(
                    user.getEmail(),
                    "BOOK_RETURN",
                    "Returned book ID: " + book.getId()
                            + "; borrowing ID: " + borrowingId,
                    "SUCCESS");

        } catch (IllegalArgumentException | IllegalStateException exception) {

            auditService.record(
                    user.getEmail(),
                    "BOOK_RETURN",
                    "Borrowing ID: " + borrowingId
                            + "; reason: " + exception.getMessage(),
                    "FAILURE");

            throw exception;
        }
    }
}