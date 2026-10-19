package com.lax.lms;

import com.lax.lms.repository.BorrowingRepository;
import com.lax.lms.repository.LibraryUserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class BorrowingController {

    private final BorrowingService borrowingService;
    private final BorrowingRepository borrowingRepository;
    private final LibraryUserRepository userRepository;

    public BorrowingController(
            BorrowingService borrowingService,
            BorrowingRepository borrowingRepository,
            LibraryUserRepository userRepository) {

        this.borrowingService = borrowingService;
        this.borrowingRepository = borrowingRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/books/{bookId}/borrow")
    public String borrowBook(
            @PathVariable Long bookId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new IllegalStateException("You must be logged in to borrow books.");
            }

            LibraryUser user = userRepository
                    .findByEmailIgnoreCase(authentication.getName())
                    .orElseThrow(() ->
                            new IllegalStateException("User account not found."));

            if (!"USER".equalsIgnoreCase(user.getRole())) {
                throw new IllegalStateException("Only regular users can borrow books.");
            }

            borrowingService.borrowBook(user.getId(), bookId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Book borrowed successfully! Please return it within 14 days.");

        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", exception.getMessage());
        }

        return "redirect:/user/borrowings";
    }

    @PostMapping("/borrowings/{borrowingId}/return")
    public String returnBook(
            @PathVariable Long borrowingId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new IllegalStateException("You must be logged in to return books.");
            }

            LibraryUser user = userRepository
                    .findByEmailIgnoreCase(authentication.getName())
                    .orElseThrow(() ->
                            new IllegalStateException("User account not found."));

            if (!"USER".equalsIgnoreCase(user.getRole())) {
                throw new IllegalStateException("Only regular users can return books.");
            }

            borrowingService.returnBook(borrowingId, user.getId());

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Book returned successfully!");

        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", exception.getMessage());
        }

        return "redirect:/user/borrowings";
    }
}
