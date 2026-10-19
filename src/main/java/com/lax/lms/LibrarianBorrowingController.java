package com.lax.lms;

import com.lax.lms.repository.BorrowingRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class LibrarianBorrowingController {

    private final BorrowingRepository borrowingRepository;

    public LibrarianBorrowingController(
            BorrowingRepository borrowingRepository) {
        this.borrowingRepository = borrowingRepository;
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
}