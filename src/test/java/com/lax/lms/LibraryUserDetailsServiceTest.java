package com.lax.lms;

import com.lax.lms.repository.LibraryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LibraryUserDetailsServiceTest {

    private LibraryUserRepository userRepository;
    private LibrarianAccessPolicy accessPolicy;
    private LibraryUserDetailsService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(LibraryUserRepository.class);
        accessPolicy = new LibrarianAccessPolicy("lakson130208@gmail.com");
        service = new LibraryUserDetailsService(userRepository, accessPolicy);
    }

    @Test
    @DisplayName("Password login for authorized email does NOT grant librarian access (strictly ROLE_USER)")
    void passwordLogin_forAuthorizedLibrarianEmail_doesNotGrantLibrarianRole() {
        String email = "lakson130208@gmail.com";
        LibraryUser user = new LibraryUser();
        user.setEmail(email);
        user.setPassword("encoded-pass");
        user.setRole("LIBRARIAN");

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(email);

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER")
                .doesNotContain("ROLE_LIBRARIAN");
    }

    @Test
    @DisplayName("Unauthorized email with LIBRARIAN role in DB is downgraded to ROLE_USER and saved")
    void unauthorizedEmail_withLibrarianInDb_isDowngradedToUser() {
        String email = "imposter@example.com";
        LibraryUser user = new LibraryUser();
        user.setEmail(email);
        user.setPassword("encoded-pass");
        user.setRole("LIBRARIAN");

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(email);

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        assertThat(user.getRole()).isEqualTo("USER");
        verify(userRepository).save(argThat(u -> "USER".equals(u.getRole())));
    }

    @Test
    @DisplayName("Standard student user receives ROLE_USER")
    void standardStudent_receivesRoleUser() {
        String email = "student@example.com";
        LibraryUser user = new LibraryUser();
        user.setEmail(email);
        user.setPassword("encoded-pass");
        user.setRole("USER");

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(email);

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Unknown email throws UsernameNotFoundException")
    void unknownEmail_throwsUsernameNotFoundException() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Account not found");
    }
}
