package com.lax.lms;

import com.lax.lms.repository.AuditLogRepository;
import com.lax.lms.repository.LibraryUserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

@Service
public class GoogleOidcUserService extends OidcUserService {

    private static final Set<String> LIBRARIAN_EMAILS = Set.of(
            "lakson130208@gmail.com",
            "lakson.s.2025.csecs@rajalakshmi.edu.in"
    );

    private final LibraryUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    public GoogleOidcUserService(
            LibraryUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogRepository auditLogRepository) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest)
            throws OAuth2AuthenticationException {

        OidcUser googleUser = super.loadUser(userRequest);

        String email = googleUser.getAttribute("email");

        Boolean emailVerified =
                googleUser.getAttribute("email_verified");

        if (email == null || !Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "Google email is missing or not verified."
            );
        }

        email = email.trim().toLowerCase();

        String finalEmail = email;

        boolean isLibrarian = LIBRARIAN_EMAILS.contains(email);

        String assignedRole = isLibrarian
                ? "LIBRARIAN"
                : "USER";

        LibraryUser libraryUser = userRepository
                .findByEmailIgnoreCase(email)
                .orElseGet(() -> {

                    LibraryUser newUser = new LibraryUser();

                    String fullName = googleUser.getAttribute("name");

                    if (fullName == null || fullName.isBlank()) {
                        fullName = finalEmail;
                    }

                    newUser.setFullName(
                            fullName.substring(
                                    0, Math.min(fullName.length(), 100)
                            )
                    );

                    newUser.setEmail(finalEmail);

                    // Google-only account:
                    // Generate an unguessable password.
                    newUser.setPassword(
                            passwordEncoder.encode(
                                    UUID.randomUUID().toString()
                            )
                    );

                    return newUser;
                });

        libraryUser.setRole(assignedRole);

        userRepository.save(libraryUser);

        AuditLog log = new AuditLog();
        log.setActorEmail(email);
        log.setAction("GOOGLE_SIGN_IN");
        log.setDetails("Google authentication completed successfully.");
        log.setOutcome("SUCCESS");
        log.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(log);

        List<org.springframework.security.core.GrantedAuthority> authorities =
	        new ArrayList<>(googleUser.getAuthorities());

        authorities.add(
                new SimpleGrantedAuthority("ROLE_" + assignedRole)
        );

        return new DefaultOidcUser(
                authorities,
                googleUser.getIdToken(),
                googleUser.getUserInfo(),
                "sub"
        );
    }
}
