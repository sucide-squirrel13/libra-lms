package com.lax.lms;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthAuditListener {

    private final AuditService auditService;

    public AuthAuditListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();

        auditService.record(
                email,
                "LOGIN_SUCCESS",
                "User logged in successfully.",
                "SUCCESS"
        );
    }

    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        String email = principal instanceof String
                ? (String) principal
                : "UNKNOWN";

        auditService.record(
                email,
                "LOGIN_FAILURE",
                "Login attempt failed.",
                "FAILURE"
        );
    }
}