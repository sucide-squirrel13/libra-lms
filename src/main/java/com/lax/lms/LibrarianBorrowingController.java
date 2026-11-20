package com.lax.lms;

import com.lax.lms.repository.BorrowingRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class LibrarianBorrowingController {

    private final BorrowingRepository borrowingRepository;
    private final BorrowingService borrowingService;

    public LibrarianBorrowingController(
            BorrowingRepository borrowingRepository,
            BorrowingService borrowingService) {
        this.borrowingRepository = borrowingRepository;
        this.borrowingService = borrowingService;
    }

    @GetMapping("/librarian/borrowings")
    public String showAllBorrowings(Model model) {

        List<Borrowing> borrowings =
                borrowingRepository.findAllByOrderByBorrowDateDesc();

        LocalDate today = LocalDate.now();

        long activeBorrowings = borrowings.stream()
                .filter(b -> "BORROWED".equals(b.getStatus()))
                .count();

        long overdueBorrowings = borrowings.stream()
                .filter(b -> "BORROWED".equals(b.getStatus()))
                .filter(b -> b.getDueDate().isBefore(today))
                .count();

        long returnedBorrowings = borrowings.stream()
                .filter(b -> "RETURNED".equals(b.getStatus()))
                .count();

        model.addAttribute("borrowings", borrowings);
        model.addAttribute("today", today);
        model.addAttribute("activeBorrowings", activeBorrowings);
        model.addAttribute("overdueBorrowings", overdueBorrowings);
        model.addAttribute("returnedBorrowings", returnedBorrowings);

        return "librarian-borrowings";
    }

    @PostMapping("/librarian/borrowings/{id}/return")
    public String returnBorrowing(
            @PathVariable("id") Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            String librarianEmail = AuthenticatedEmailResolver.resolve(authentication);
            if (librarianEmail == null) {
                throw new IllegalStateException("Librarian account not found.");
            }

            borrowingService.returnBookByLibrarian(id, librarianEmail);
            redirectAttributes.addFlashAttribute(
                    "message", "Book marked as returned successfully.");

        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", exception.getMessage());
        }

        return "redirect:/librarian/borrowings";
    }
}