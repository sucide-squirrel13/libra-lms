package com.lax.lms;

import com.lax.lms.repository.LibraryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for SEC-04: the global model advice must recognise
 * Google OIDC-authenticated users exactly like form-login users.
 *
 * The OIDC principal's name attribute is "sub", so Authentication.getName()
 * is NOT an email address; the email claim must be resolved instead, or the
 * navbar user navigation is silently missing for Google sign-ins.
 */
class GlobalModelAdviceTest {

    private LibraryUserRepository userRepository;
    private GlobalModelAdvice advice;
    private LibraryUser regularUser;

    @BeforeEach
    void setUp() {
        userRepository = mock(LibraryUserRepository.class);
        advice = new GlobalModelAdvice(userRepository);

        regularUser = new LibraryUser();
        regularUser.setFullName("Regular Reader");
        regularUser.setEmail("reader@example.com");
        regularUser.setRole("USER");

        when(userRepository.findByEmailIgnoreCase("reader@example.com"))
                .thenReturn(Optional.of(regularUser));
        // The pre-fix behaviour would have looked up the OIDC subject as "an email", which maps to nobody.
        when(userRepository.findByEmailIgnoreCase("google-subject-123"))
                .thenReturn(Optional.empty());

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("SEC-04: form-login principal resolves the stored user into the model")
    void formLoginPrincipal_addsUserToModel() {
        User principal = new User(
                "reader@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, "password", principal.getAuthorities());

        Model model = new ExtendedModelMap();
        advice.addLoggedInUser(authentication, model);

        assertSame(regularUser, model.getAttribute("user"));
    }

    @Test
    @DisplayName("SEC-04: Google OIDC principal resolves the stored user into the model (email claim, not sub)")
    void oidcPrincipal_addsUserToModel() {
        Map<String, Object> claims = Map.of(
                "sub", "google-subject-123",
                "email", "reader@example.com",
                "email_verified", true,
                "name", "Regular Reader");
        Instant now = Instant.now();
        OidcIdToken idToken =
                new OidcIdToken("id-token-value", now, now.plusSeconds(3600), claims);
        OidcUserInfo userInfo = new OidcUserInfo(claims);
        DefaultOidcUser principal = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, userInfo, "sub");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google");

        Model model = new ExtendedModelMap();
        advice.addLoggedInUser(authentication, model);

        assertSame(regularUser, model.getAttribute("user"));
    }

    @Test
    @DisplayName("SEC-04: anonymous authentication adds nothing to the model")
    void anonymousAuthentication_addsNothingToModel() {
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        Model model = new ExtendedModelMap();
        advice.addLoggedInUser(authentication, model);

        assertFalse(model.containsAttribute("user"));
    }

    @Test
    @DisplayName("SEC-04: null authentication adds nothing to the model")
    void nullAuthentication_addsNothingToModel() {
        Model model = new ExtendedModelMap();
        advice.addLoggedInUser(null, model);

        assertFalse(model.containsAttribute("user"));
    }
}
