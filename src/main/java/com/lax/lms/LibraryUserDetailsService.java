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

    public LibraryUserDetailsService(LibraryUserRepository userRepository) {
        this.userRepository = userRepository;
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

        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        return new User(
                libraryUser.getEmail(),
                libraryUser.getPassword(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}