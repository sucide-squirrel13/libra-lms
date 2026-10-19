package com.lax.lms;

import com.lax.lms.repository.BookRepository;
import com.lax.lms.repository.BorrowingRepository;
import com.lax.lms.repository.LibraryUserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class LibrarianDashboardService {

    private final BookRepository bookRepository;
    private final BorrowingRepository borrowingRepository;
    private final LibraryUserRepository userRepository;

    public LibrarianDashboardService(
            BookRepository bookRepository,
            BorrowingRepository borrowingRepository,
            LibraryUserRepository userRepository) {

        this.bookRepository = bookRepository;
        this.borrowingRepository = borrowingRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStats getStatistics() {

        long totalTitles = bookRepository.count();
        long totalCopies = bookRepository.countTotalCopies();
        long availableCopies = bookRepository.countAvailableCopies();

        long activeBorrowings =
                borrowingRepository.countByStatus("BORROWED");

        long overdueBorrowings =
                borrowingRepository.countByStatusAndDueDateBefore(
                        "BORROWED", LocalDate.now());

        long registeredUsers = userRepository.count();

        return new DashboardStats(
                totalTitles,
                totalCopies,
                availableCopies,
                activeBorrowings,
                overdueBorrowings,
                registeredUsers
        );
    }

    public record DashboardStats(
            long totalTitles,
            long totalCopies,
            long availableCopies,
            long activeBorrowings,
            long overdueBorrowings,
            long registeredUsers) {
    }
}