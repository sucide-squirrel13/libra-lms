package com.lax.lms;

import com.lax.lms.repository.AuditLogRepository;
import com.lax.lms.repository.LibraryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleOidcUserServiceTest {

    private LibraryUserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuditLogRepository auditLogRepository;
    private GoogleOidcUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(LibraryUserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditLogRepository = mock(AuditLogRepository.class);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");

        service = new GoogleOidcUserService(userRepository, passwordEncoder, auditLogRepository);
    }

    private OidcUser createGoogleUser(String email, Boolean emailVerified, String name) {
        Map<String, Object> claims = Map.of(
                "sub", "google-sub-" + email.hashCode(),
                "email", email,
                "email_verified", emailVerified != null && emailVerified,
                "name", name
        );
        Instant now = Instant.now();
        OidcIdToken idToken = new OidcIdToken("token", now, now.plusSeconds(3600), claims);
        OidcUserInfo userInfo = new OidcUserInfo(claims);
        return new DefaultOidcUser(Collections.emptyList(), idToken, userInfo, "sub");
    }

    @Test
    @DisplayName("Existing USER in database retains USER role upon Google login")
    void existingUser_retainsUserRole() {
        String email = "existing.reader@example.com";
        LibraryUser existingUser = new LibraryUser();
        existingUser.setEmail(email);
        existingUser.setFullName("Existing Reader");
        existingUser.setRole("USER");

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(existingUser));

        OidcUser googleUser = createGoogleUser(email, true, "Existing Reader");
        OidcUser result = service.processOidcUser(googleUser);

        assertThat(existingUser.getRole()).isEqualTo("USER");
        assertThat(result.getAuthorities())
                .extracting("authority")
                .contains("ROLE_USER")
                .doesNotContain("ROLE_LIBRARIAN");

        verify(userRepository).save(argThat(user -> "USER".equals(user.getRole())));
    }

    @Test
    @DisplayName("Existing LIBRARIAN in database retains LIBRARIAN role even if not in hardcoded allowlist")
    void existingLibrarian_notInAllowlist_retainsLibrarianRole() {
        String email = "custom.librarian@college.edu";
        LibraryUser existingLibrarian = new LibraryUser();
        existingLibrarian.setEmail(email);
        existingLibrarian.setFullName("Custom Librarian");
        existingLibrarian.setRole("LIBRARIAN");

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(existingLibrarian));

        OidcUser googleUser = createGoogleUser(email, true, "Custom Librarian");
        OidcUser result = service.processOidcUser(googleUser);

        assertThat(existingLibrarian.getRole()).isEqualTo("LIBRARIAN");
        assertThat(result.getAuthorities())
                .extracting("authority")
                .contains("ROLE_LIBRARIAN")
                .doesNotContain("ROLE_USER");

        verify(userRepository).save(argThat(user -> "LIBRARIAN".equals(user.getRole())));
    }

    @Test
    @DisplayName("New Google user not in allowlist receives default USER role")
    void newGoogleUser_notInAllowlist_receivesUserRole() {
        String email = "new.student@example.com";
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());

        OidcUser googleUser = createGoogleUser(email, true, "New Student");
        OidcUser result = service.processOidcUser(googleUser);

        assertThat(result.getAuthorities())
                .extracting("authority")
                .contains("ROLE_USER")
                .doesNotContain("ROLE_LIBRARIAN");

        verify(userRepository).save(argThat(user ->
                email.equals(user.getEmail()) && "USER".equals(user.getRole())));
    }

    @Test
    @DisplayName("New Google user in allowlist receives LIBRARIAN role")
    void newGoogleUser_inAllowlist_receivesLibrarianRole() {
        String email = "lakson130208@gmail.com";
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());

        OidcUser googleUser = createGoogleUser(email, true, "Lakson S");
        OidcUser result = service.processOidcUser(googleUser);

        assertThat(result.getAuthorities())
                .extracting("authority")
                .contains("ROLE_LIBRARIAN")
                .doesNotContain("ROLE_USER");

        verify(userRepository).save(argThat(user ->
                email.equals(user.getEmail()) && "LIBRARIAN".equals(user.getRole())));
    }

    @Test
    @DisplayName("Google user with missing or unverified email throws OAuth2AuthenticationException")
    void unverifiedEmail_throwsOAuth2AuthenticationException() {
        OidcUser googleUser = createGoogleUser("unverified@example.com", false, "Unverified User");

        assertThatThrownBy(() -> service.processOidcUser(googleUser))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("missing or not verified");
    }
}
