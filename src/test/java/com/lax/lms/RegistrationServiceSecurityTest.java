package com.lax.lms;

import com.lax.lms.repository.AuditLogRepository;
import com.lax.lms.repository.LibraryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationServiceSecurityTest {

    private LibraryUserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuditLogRepository auditLogRepository;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(LibraryUserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditLogRepository = mock(AuditLogRepository.class);
        when(passwordEncoder.encode(any())).thenReturn("hashed_password");

        registrationService = new RegistrationService(userRepository, passwordEncoder, auditLogRepository);
    }

    @Test
    @DisplayName("Registering with authorized librarian email via normal registration form must assign ROLE_USER")
    void registeringWithLibrarianEmail_assignsUserRole() {
        String email = "lakson130208@gmail.com";
        when(userRepository.existsByEmailIgnoreCase(email)).thenReturn(false);

        registrationService.register("Lakson Candidate", email, "SecurePassword123!");

        ArgumentCaptor<LibraryUser> userCaptor = ArgumentCaptor.forClass(LibraryUser.class);
        verify(userRepository).save(userCaptor.capture());

        LibraryUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getRole()).isEqualTo("USER");
        assertThat(savedUser.getRole()).isNotEqualTo("LIBRARIAN");
    }

    @Test
    @DisplayName("Registering any standard student email assigns ROLE_USER")
    void registeringStandardStudent_assignsUserRole() {
        String email = "regular.scholar@example.com";
        when(userRepository.existsByEmailIgnoreCase(email)).thenReturn(false);

        registrationService.register("Regular Scholar", email, "SecurePassword123!");

        ArgumentCaptor<LibraryUser> userCaptor = ArgumentCaptor.forClass(LibraryUser.class);
        verify(userRepository).save(userCaptor.capture());

        LibraryUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getRole()).isEqualTo("USER");
    }
}
