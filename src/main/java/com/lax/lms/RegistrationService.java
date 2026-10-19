package com.lax.lms;

import com.lax.lms.repository.AuditLogRepository;
import com.lax.lms.repository.LibraryUserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RegistrationService {

    private final LibraryUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    public RegistrationService(
            LibraryUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogRepository auditLogRepository) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void register(String fullName, String email, String password) {

        fullName = fullName.trim();
        email = email.trim().toLowerCase();

        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("All fields are required.");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters.");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "This email is already registered.");
        }

        LibraryUser user = new LibraryUser();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");

        userRepository.save(user);

        AuditLog log = new AuditLog();
        log.setActorEmail(email);
        log.setAction("USER_REGISTRATION");
        log.setDetails("A new library user account was registered.");
        log.setOutcome("SUCCESS");
        log.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(log);
    }
}