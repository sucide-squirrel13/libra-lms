package com.lax.lms;

import com.lax.lms.repository.LibraryUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LibraryUserDetailsService implements UserDetailsService {

    private final LibraryUserRepository userRepository;
    private final LibrarianAccessPolicy librarianAccessPolicy;

    public LibraryUserDetailsService(
            LibraryUserRepository userRepository,
            LibrarianAccessPolicy librarianAccessPolicy) {
        this.userRepository = userRepository;
        this.librarianAccessPolicy = librarianAccessPolicy;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        LibraryUser libraryUser = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Account not found"));

        String role = libraryUser.getRole();

        if (role == null || role.isBlank()) {
            throw new UsernameNotFoundException("Account has no assigned role");
        }

        // If an unauthorized user somehow holds the LIBRARIAN role in the database,
        // sanitize the database record by demoting it back to USER.
        if ("LIBRARIAN".equalsIgnoreCase(role) || "ROLE_LIBRARIAN".equalsIgnoreCase(role)) {
            if (!librarianAccessPolicy.isAuthorizedLibrarian(libraryUser.getEmail())) {
                libraryUser.setRole("USER");
                userRepository.save(libraryUser);
            }
        }

        // CRITICAL SECURITY ENFORCEMENT:
        // Password-based authentication MUST NEVER grant ROLE_LIBRARIAN.
        // Librarian privileges are strictly granted ONLY via verified Google OAuth authentication
        // (with email_verified=true and matching the configured allowlist).
        // Therefore, ALL password-authenticated sessions receive ROLE_USER.
        return new User(
                libraryUser.getEmail(),
                libraryUser.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}