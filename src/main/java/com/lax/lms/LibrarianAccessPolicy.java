package com.lax.lms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centralized authorization policy for the LIBRARIAN role.
 * Only accounts configured via the environment variable LIBRARIAN_EMAILS
 * (or the 'librarian.emails' configuration property) are permitted to receive
 * or retain the LIBRARIAN role.
 */
@Service
public class LibrarianAccessPolicy {

    public static final String ROLE_LIBRARIAN = "LIBRARIAN";
    public static final String ROLE_USER = "USER";

    private final Set<String> authorizedEmails;

    public LibrarianAccessPolicy(
            @Value("${librarian.emails:${LIBRARIAN_EMAILS:lakson130208@gmail.com}}") String rawEmails) {

        if (rawEmails == null || rawEmails.isBlank()) {
            this.authorizedEmails = Collections.emptySet();
        } else {
            this.authorizedEmails = Arrays.stream(rawEmails.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    /**
     * Checks if the given email address is authorized to hold the LIBRARIAN role.
     * Evaluates case-insensitively.
     *
     * @param email the email address to evaluate
     * @return true if authorized, false otherwise
     */
    public boolean isAuthorizedLibrarian(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return authorizedEmails.contains(email.trim().toLowerCase());
    }

    /**
     * Resolves the authoritative role for an account.
     * Only authorized emails with a verified Google email address can receive LIBRARIAN.
     * All other accounts receive USER.
     *
     * @param email         the user's email address
     * @param emailVerified whether the email has been verified by the provider
     * @return "LIBRARIAN" if verified and authorized; "USER" otherwise
     */
    public String resolveRole(String email, Boolean emailVerified) {
        if (Boolean.TRUE.equals(emailVerified) && isAuthorizedLibrarian(email)) {
            return ROLE_LIBRARIAN;
        }
        return ROLE_USER;
    }
}
