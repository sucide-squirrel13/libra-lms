package com.lax.lms;

import com.lax.lms.repository.LibraryUserRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class LibrarianUserController {

    private final LibraryUserRepository userRepository;

    public LibrarianUserController(
            LibraryUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/librarian/users")
    public String showUsers(Model model) {

        List<LibraryUser> users = userRepository.findAll();

        long totalUsers = users.stream()
                .filter(user -> "USER".equals(user.getRole()))
                .count();

        long librarianCount = users.stream()
                .filter(user -> "LIBRARIAN".equals(user.getRole()))
                .count();

        model.addAttribute("users", users);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("librarianCount", librarianCount);

        return "librarian-users";
    }
}