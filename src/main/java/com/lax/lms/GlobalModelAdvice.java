package com.lax.lms;

import com.lax.lms.repository.LibraryUserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {

    private final LibraryUserRepository userRepository;

    public GlobalModelAdvice(LibraryUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute
    public void addLoggedInUser(
            Authentication authentication,
            Model model) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().equals("anonymousUser")) {
            return;
        }

        userRepository.findByEmailIgnoreCase(authentication.getName())
                .ifPresent(user -> model.addAttribute("user", user));
    }
}