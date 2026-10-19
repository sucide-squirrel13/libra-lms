package com.lax.lms;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/register")
    public String showRegistrationPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) {

        try {
            registrationService.register(fullName, email, password);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Registration successful! You can now log in.");

            return "redirect:/login";

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", exception.getMessage());

            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model,
            HttpServletRequest request) {

        CsrfToken csrfToken =
                (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (csrfToken != null) {
            csrfToken.getToken();
        }

        if (error != null) {
            model.addAttribute(
                    "errorMessage", "Invalid email or password.");
        }

        if (logout != null) {
            model.addAttribute(
                    "successMessage", "You have logged out successfully.");
        }

        return "login";
    }
}