package com.lax.lms;

import com.lax.lms.repository.LibraryUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
public class SettingsController {

    private final LibraryUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SettingsController(LibraryUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/settings")
    public String showSettings(Authentication authentication, Model model) {
        String email = AuthenticatedEmailResolver.resolve(authentication);
        if (email == null) {
            return "redirect:/login";
        }

        Optional<LibraryUser> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        LibraryUser user = userOpt.get();
        boolean isLibrarian = "LIBRARIAN".equalsIgnoreCase(user.getRole());
        boolean isGoogleUser = user.getPassword() != null && user.getPassword().startsWith("OAUTH2_");

        model.addAttribute("user", user);
        model.addAttribute("isLibrarian", isLibrarian);
        model.addAttribute("isGoogleUser", isGoogleUser);
        model.addAttribute("activePage", "settings");

        return "settings";
    }

    @PostMapping("/settings/preferences")
    public String updatePreferences(
            @RequestParam(value = "themePreference", defaultValue = "dark") String themePreference,
            @RequestParam(value = "accentColor", defaultValue = "gold") String accentColor,
            @RequestParam(value = "interfaceDensity", defaultValue = "comfortable") String interfaceDensity,
            @RequestParam(value = "fontSizePreference", defaultValue = "normal") String fontSizePreference,
            @RequestParam(value = "contentWidth", defaultValue = "standard") String contentWidth,
            @RequestParam(value = "readingLayout", defaultValue = "grid") String readingLayout,
            @RequestParam(value = "emailBorrowReminders", defaultValue = "false") boolean emailBorrowReminders,
            @RequestParam(value = "emailDueReminders", defaultValue = "false") boolean emailDueReminders,
            @RequestParam(value = "emailReturnConfirmations", defaultValue = "false") boolean emailReturnConfirmations,
            @RequestParam(value = "reducedMotionPreference", defaultValue = "false") boolean reducedMotionPreference,
            @RequestParam(value = "highContrastPreference", defaultValue = "false") boolean highContrastPreference,
            @RequestParam(value = "defaultLandingPage", defaultValue = "dashboard") String defaultLandingPage,
            @RequestParam(value = "dateFormatPreference", defaultValue = "YYYY-MM-DD") String dateFormatPreference,
            @RequestParam(value = "timezonePreference", defaultValue = "UTC") String timezonePreference,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String email = AuthenticatedEmailResolver.resolve(authentication);
        if (email == null) {
            return "redirect:/login";
        }

        Optional<LibraryUser> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        LibraryUser user = userOpt.get();
        user.setThemePreference(themePreference);
        user.setAccentColor(accentColor);
        user.setInterfaceDensity(interfaceDensity);
        user.setFontSizePreference(fontSizePreference);
        user.setContentWidth(contentWidth);
        user.setReadingLayout(readingLayout);
        user.setEmailBorrowReminders(emailBorrowReminders);
        user.setEmailDueReminders(emailDueReminders);
        user.setEmailReturnConfirmations(emailReturnConfirmations);
        user.setReducedMotionPreference(reducedMotionPreference);
        user.setHighContrastPreference(highContrastPreference);
        user.setDefaultLandingPage(defaultLandingPage);
        user.setDateFormatPreference(dateFormatPreference);
        user.setTimezonePreference(timezonePreference);

        userRepository.save(user);

        redirectAttributes.addFlashAttribute("settingsSuccess", "Your preferences have been saved successfully.");
        return "redirect:/settings";
    }

    @PostMapping("/settings/account")
    public String updateAccount(
            @RequestParam("fullName") String fullName,
            @RequestParam(value = "currentPassword", required = false) String currentPassword,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String email = AuthenticatedEmailResolver.resolve(authentication);
        if (email == null) {
            return "redirect:/login";
        }

        Optional<LibraryUser> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        LibraryUser user = userOpt.get();

        if (fullName == null || fullName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("accountError", "Full name cannot be blank.");
            return "redirect:/settings";
        }
        user.setFullName(fullName.trim());

        // Password change handling
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            boolean isGoogleUser = user.getPassword() != null && user.getPassword().startsWith("OAUTH2_");
            if (isGoogleUser) {
                redirectAttributes.addFlashAttribute("accountError", "Password changes are managed through Google for this account.");
                userRepository.save(user);
                return "redirect:/settings";
            }

            if (currentPassword == null || currentPassword.isEmpty() || !passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("accountError", "Current password does not match.");
                return "redirect:/settings";
            }

            if (newPassword.trim().length() < 6) {
                redirectAttributes.addFlashAttribute("accountError", "New password must be at least 6 characters.");
                return "redirect:/settings";
            }

            user.setPassword(passwordEncoder.encode(newPassword.trim()));
        }

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("accountSuccess", "Account details updated successfully.");
        return "redirect:/settings";
    }

    @PostMapping("/api/settings/theme")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleThemeAjax(
            @RequestParam("theme") String theme,
            Authentication authentication) {

        if (!"light".equalsIgnoreCase(theme) && !"dark".equalsIgnoreCase(theme) && !"system".equalsIgnoreCase(theme)) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid theme"));
        }

        String email = AuthenticatedEmailResolver.resolve(authentication);
        if (email != null) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                user.setThemePreference(theme.toLowerCase());
                userRepository.save(user);
            });
        }

        return ResponseEntity.ok(Map.of("status", "ok", "theme", theme.toLowerCase()));
    }
}
