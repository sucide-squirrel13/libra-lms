package com.lax.lms;

import com.lax.lms.repository.AuditLogRepository;
import com.lax.lms.repository.BookRepository;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Controller
public class HomeController {

    private final BookRepository bookRepository;
    private final AuditLogRepository auditLogRepository;
    private final LibrarianDashboardService dashboardService;

    public HomeController(
            BookRepository bookRepository,
            AuditLogRepository auditLogRepository,
            LibrarianDashboardService dashboardService) {
        this.bookRepository = bookRepository;
        this.auditLogRepository = auditLogRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String home(Model model) {
        var featuredBooks = bookRepository.findAll()
                .stream()
                .filter(b -> b.getAvailableCopies() > 0)
                .limit(4)
                .toList();
        model.addAttribute("featuredBooks", featuredBooks);
        model.addAttribute("totalBooks", bookRepository.count());
        model.addAttribute("totalCopies", bookRepository.countTotalCopies());
        return "home";
    }

    @GetMapping("/librarian")
    public String librarian(Model model) {

            var books = bookRepository.findAll();

        var stats = dashboardService.getStatistics();

        model.addAttribute("books", books);

    // Preserve the existing attributes used by librarian.html.
        model.addAttribute("totalBooks", stats.totalCopies());
        model.addAttribute("availableBooks", stats.availableCopies());

    // New dashboard statistics.
        model.addAttribute("totalTitles", stats.totalTitles());
        model.addAttribute("totalCopies", stats.totalCopies());
        model.addAttribute("availableCopies", stats.availableCopies());
        model.addAttribute("activeBorrowings", stats.activeBorrowings());
        model.addAttribute("overdueBorrowings", stats.overdueBorrowings());
        model.addAttribute("registeredUsers", stats.registeredUsers());

        return "librarian";
}

    @GetMapping("/librarian/books/new")
    public String showAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        return "add-book";
    }

    @PostMapping("/librarian/books")
    @Transactional
    public String saveBook(
            @Valid @ModelAttribute("book") Book book,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "add-book";
        }

        book.setAvailableCopies(book.getQuantity());
        Book savedBook = bookRepository.save(book);

        recordAudit(
                "BOOK_CREATED",
                "Created book ID: " + savedBook.getId()
                        + ", title: " + savedBook.getTitle(),
                "SUCCESS"
        );

        redirectAttributes.addFlashAttribute(
                "message", "Book added successfully."
        );

        return "redirect:/librarian";
    }

    @GetMapping("/librarian/books/edit/{id}")
    public String showEditBookForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        var book = bookRepository.findById(id);

        if (book.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "message", "Book not found."
            );
            return "redirect:/librarian";
        }

        model.addAttribute("book", book.get());
        return "edit-book";
    }

    @PostMapping("/librarian/books/update/{id}")
    @Transactional
    public String updateBook(
            @PathVariable Long id,
            @Valid @ModelAttribute("book") Book formBook,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "edit-book";
        }

        var existingBook = bookRepository.findById(id);

        if (existingBook.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "message", "Book not found."
            );
            return "redirect:/librarian";
        }

        Book book = existingBook.get();

        book.setTitle(formBook.getTitle());
        book.setAuthor(formBook.getAuthor());
        book.setIsbn(formBook.getIsbn());
        book.setCategory(formBook.getCategory());

        int unavailableCopies =
                book.getQuantity() - book.getAvailableCopies();

        if (formBook.getQuantity() < unavailableCopies) {
            result.rejectValue(
                    "quantity",
                    "quantity.tooSmall",
                    "Quantity cannot be less than copies currently issued."
            );
            return "edit-book";
        }

        book.setQuantity(formBook.getQuantity());
        book.setAvailableCopies(
                formBook.getQuantity() - unavailableCopies
        );

        bookRepository.save(book);

        recordAudit(
                "BOOK_UPDATED",
                "Updated book ID: " + book.getId()
                        + ", title: " + book.getTitle(),
                "SUCCESS"
        );

        redirectAttributes.addFlashAttribute(
                "message", "Book updated successfully."
        );

        return "redirect:/librarian";
    }

    @PostMapping("/librarian/books/delete/{id}")
    @Transactional
    public String deleteBook(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        var existingBook = bookRepository.findById(id);

        if (existingBook.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "message", "Book not found."
            );
            return "redirect:/librarian";
        }

        Book book = existingBook.get();

        String details = "Deleted book ID: " + book.getId()
                + ", title: " + book.getTitle();

        bookRepository.delete(book);

        recordAudit(
                "BOOK_DELETED",
                details,
                "SUCCESS"
        );

        redirectAttributes.addFlashAttribute(
                "message", "Book deleted successfully."
        );

        return "redirect:/librarian";
    }

    private void recordAudit(
            String action,
            String details,
            String outcome) {

        AuditLog log = new AuditLog();

        log.setActorEmail("LIBRARIAN");
        log.setAction(action);
        log.setDetails(details);
        log.setOutcome(outcome);
        log.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(log);
    }
}