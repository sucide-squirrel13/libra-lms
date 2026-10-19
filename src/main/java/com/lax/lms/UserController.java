package com.lax.lms;

import com.lax.lms.repository.BookRepository;
import com.lax.lms.repository.BorrowingRepository;
import com.lax.lms.repository.LibraryUserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class UserController {

    private final LibraryUserRepository userRepository;
    private final BorrowingRepository borrowingRepository;
    private final BookRepository bookRepository;

    public UserController(
            LibraryUserRepository userRepository,
            BorrowingRepository borrowingRepository,
            BookRepository bookRepository) {

        this.userRepository = userRepository;
        this.borrowingRepository = borrowingRepository;
        this.bookRepository = bookRepository;
    }

    @GetMapping("/user/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model) {

        String email = authentication.getName();

        LibraryUser user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user not found."));

        List<Borrowing> borrowings =
                borrowingRepository
                        .findByUserIdOrderByBorrowDateDesc(user.getId());

        LocalDate today = LocalDate.now();

        long activeBorrowings = borrowings.stream()
                .filter(b -> "BORROWED".equals(b.getStatus()))
                .count();

        long overdueBorrowings = borrowings.stream()
                .filter(b -> "BORROWED".equals(b.getStatus()))
                .filter(b -> b.getDueDate() != null)
                .filter(b -> b.getDueDate().isBefore(today))
                .count();

        List<Borrowing> recentBorrowings = borrowings.stream()
                .limit(5)
                .toList();

        List<Book> featuredBooks = bookRepository.findAll()
                .stream()
                .filter(b -> b.getAvailableCopies() > 0)
                .limit(4)
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("activeBorrowings", activeBorrowings);
        model.addAttribute("overdueBorrowings", overdueBorrowings);
        model.addAttribute("recentBorrowings", recentBorrowings);
        model.addAttribute("featuredBooks", featuredBooks);
        model.addAttribute("totalBorrowingRecords", borrowings.size());

        return "user-dashboard";
    }

    @GetMapping("/user/borrowings")
    public String borrowings(
            Authentication authentication,
            Model model) {

        String email = authentication.getName();

        LibraryUser user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user not found."));

        List<Borrowing> borrowings =
                borrowingRepository
                        .findByUserIdOrderByBorrowDateDesc(user.getId());

        LocalDate today = LocalDate.now();

        List<Borrowing> activeBorrowings = borrowings.stream()
                .filter(b -> "BORROWED".equals(b.getStatus()))
                .toList();

        List<Borrowing> overdueBorrowings = activeBorrowings.stream()
                .filter(b -> b.getDueDate() != null)
                .filter(b -> b.getDueDate().isBefore(today))
                .toList();

        List<Borrowing> returnedBorrowings = borrowings.stream()
                .filter(b -> "RETURNED".equals(b.getStatus()))
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("activeBorrowings", activeBorrowings);
        model.addAttribute("overdueBorrowings", overdueBorrowings);
        model.addAttribute("returnedBorrowings", returnedBorrowings);
        model.addAttribute("totalBorrowingRecords", borrowings.size());

        return "user-borrowings";
    }
}