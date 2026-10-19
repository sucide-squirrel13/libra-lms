package com.lax.lms;

import com.lax.lms.repository.BookRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class BookCatalogueController {

    private final BookRepository bookRepository;

    public BookCatalogueController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/books")
    public String showCatalogue(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String category,
            Model model) {

        keyword = keyword.trim();
        category = category.trim();

        List<Book> books =
                bookRepository.searchCatalogue(keyword, category);

        List<String> categories = bookRepository.findAll()
                .stream()
                .map(Book::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        model.addAttribute("books", books);
        model.addAttribute("categories", categories);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("totalResults", books.size());

        return "book-catalogue";
    }
}