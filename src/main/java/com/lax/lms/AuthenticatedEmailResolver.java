package com.lax.lms;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Resolves the email address of the authenticated user, regardless of how
 * they signed in.
 *
 * For the form login, the principal is a UserDetails whose username is the
 * stored account email, so {@link Authentication#getName()} is the email.
 * For a Google OIDC login, the principal is a DefaultOidcUser whose name
 * attribute key is "sub", so {@link Authentication#getName()} is the OIDC
 * subject - NOT the email. The email claim must be read instead.
 */
final class AuthenticatedEmailResolver {

    private AuthenticatedEmailResolver() {
    }

    /**
     * @param authentication the current authentication, may be null
     * @return the user's email address, or {@code null} when there is no
     *         authenticated user or no usable email address
     */
    static String resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            if (email == null) {
                return null;
            }
            email = email.trim();
            return email.isBlank() ? null : email;
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }
}
