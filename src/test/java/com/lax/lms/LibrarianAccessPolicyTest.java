package com.lax.lms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LibrarianAccessPolicyTest {

    @Test
    @DisplayName("Authorized email matches case-insensitively and with whitespace")
    void authorizedEmailMatchesCaseInsensitively() {
        LibrarianAccessPolicy policy = new LibrarianAccessPolicy("  lakson130208@gmail.com  ");

        assertThat(policy.isAuthorizedLibrarian("lakson130208@gmail.com")).isTrue();
        assertThat(policy.isAuthorizedLibrarian("LAKSON130208@GMAIL.COM")).isTrue();
        assertThat(policy.isAuthorizedLibrarian("Lakson130208@Gmail.Com")).isTrue();
        assertThat(policy.isAuthorizedLibrarian("  lakson130208@gmail.com  ")).isTrue();
    }

    @Test
    @DisplayName("Unauthorized email addresses are rejected")
    void unauthorizedEmailIsRejected() {
        LibrarianAccessPolicy policy = new LibrarianAccessPolicy("lakson130208@gmail.com");

        assertThat(policy.isAuthorizedLibrarian("other.student@example.com")).isFalse();
        assertThat(policy.isAuthorizedLibrarian("lakson@gmail.com")).isFalse();
        assertThat(policy.isAuthorizedLibrarian("lakson.s.2025.csecs@rajalakshmi.edu.in")).isFalse();
        assertThat(policy.isAuthorizedLibrarian("")).isFalse();
        assertThat(policy.isAuthorizedLibrarian(null)).isFalse();
    }

    @Test
    @DisplayName("Comma-separated email list is parsed and normalized correctly")
    void commaSeparatedEmailList() {
        LibrarianAccessPolicy policy = new LibrarianAccessPolicy("lakson130208@gmail.com, admin@college.edu");

        assertThat(policy.isAuthorizedLibrarian("lakson130208@gmail.com")).isTrue();
        assertThat(policy.isAuthorizedLibrarian("admin@college.edu")).isTrue();
        assertThat(policy.isAuthorizedLibrarian("student@college.edu")).isFalse();
    }

    @Test
    @DisplayName("resolveRole assigns LIBRARIAN only when verified and authorized")
    void resolveRoleAssignsLibrarianOnlyWhenVerifiedAndAuthorized() {
        LibrarianAccessPolicy policy = new LibrarianAccessPolicy("lakson130208@gmail.com");

        // Verified and authorized -> LIBRARIAN
        assertThat(policy.resolveRole("lakson130208@gmail.com", true)).isEqualTo("LIBRARIAN");
        assertThat(policy.resolveRole("LAKSON130208@GMAIL.COM", true)).isEqualTo("LIBRARIAN");

        // Unverified but authorized -> USER
        assertThat(policy.resolveRole("lakson130208@gmail.com", false)).isEqualTo("USER");
        assertThat(policy.resolveRole("lakson130208@gmail.com", null)).isEqualTo("USER");

        // Verified but unauthorized -> USER
        assertThat(policy.resolveRole("other@example.com", true)).isEqualTo("USER");

        // Unverified and unauthorized -> USER
        assertThat(policy.resolveRole("other@example.com", false)).isEqualTo("USER");
    }
}
