package com.lax.lms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AuthAuditListenerTest {

    private AuditService auditService;
    private AuthAuditListener listener;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        listener = new AuthAuditListener(auditService);
    }

    @Test
    @DisplayName("onLoginSuccess for form login records the user email as actor")
    void onLoginSuccess_formLogin_recordsUserEmail() {
        User principal = new User(
                "reader@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, "password", principal.getAuthorities());

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);
        listener.onLoginSuccess(event);

        verify(auditService).record(
                eq("reader@example.com"),
                eq("LOGIN_SUCCESS"),
                eq("User logged in successfully."),
                eq("SUCCESS")
        );
    }

    @Test
    @DisplayName("onLoginSuccess for Google OIDC login records the email claim, NOT the sub identifier")
    void onLoginSuccess_googleOidcLogin_recordsEmailClaimNotSub() {
        Map<String, Object> claims = Map.of(
                "sub", "google-sub-1092837465",
                "email", "google.reader@example.com",
                "email_verified", true,
                "name", "Google Reader"
        );
        Instant now = Instant.now();
        OidcIdToken idToken = new OidcIdToken("token-val", now, now.plusSeconds(3600), claims);
        OidcUserInfo userInfo = new OidcUserInfo(claims);
        DefaultOidcUser principal = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                idToken,
                userInfo,
                "sub"
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google"
        );

        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authentication);
        listener.onLoginSuccess(event);

        verify(auditService).record(
                eq("google.reader@example.com"),
                eq("LOGIN_SUCCESS"),
                eq("User logged in successfully."),
                eq("SUCCESS")
        );
        verify(auditService, never()).record(
                eq("google-sub-1092837465"),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("onLoginFailure records failure audit with principal string")
    void onLoginFailure_recordsFailureAudit() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("unknown@example.com", "wrong-password");

        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(
                        authentication,
                        new BadCredentialsException("Bad credentials")
                );

        listener.onLoginFailure(event);

        verify(auditService).record(
                eq("unknown@example.com"),
                eq("LOGIN_FAILURE"),
                eq("Login attempt failed."),
                eq("FAILURE")
        );
    }
}
