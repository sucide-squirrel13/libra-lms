# LIBRA LMS Functional Audit

## Executive Summary
This audit identifies critical security vulnerabilities, functional inconsistencies, and UI/UX issues in the LIBRA LMS codebase. The system preserves its Spring Boot/Java/Thymeleaf/PostgreSQL architecture while requiring urgent security fixes and UI standardization.

## Confirmed Security Concerns

### 1. Hardcoded Librarian Allowlist
**Location:** `GoogleOidcUserService.java` (lines 26-29)
```java
private static final Set<String> LIBRARIAN_EMAILS = Set.of(
    "lakson130208@gmail.com",
    "lakson.s.2025.csecs@rajalakshmi.edu.in"
);
```
- **Issue:** Hardcoded email allowlist creates single point of failure
- **Risk:** New librarian accounts require code deployment
- **Recommendation:** Move to database-backed role management

### 2. OIDC Authentication Flaw
**Location:** `AuthAuditListener.java` (lines 19-27)
```java
String email = event.getAuthentication().getName();
```
- **Issue:** Uses `getName()` (sub claim) instead of email claim for authentication
- **Risk:** Authenticated user email may not match library user email
- **Recommendation:** Implement proper email extraction from OIDC user info

### 3. Credential Exposure
**Location:** `application-local.properties` (lines 22-36)
- **Issue:** Contains real Google OAuth credentials (client-secret GOCSPX-..., password 2008)
- **Risk:** Credentials exposed in source control if not properly managed
- **Recommendation:** Ensure .gitignore excludes local config files and use environment variables

## Functional Inconsistencies

### 1. Book Availability Discrepancy
- **Home Page:** Shows 4 available copies for "The C Programming Language"
- **Manage Books Page:** Shows 4 available copies for same book
- **Affected Files:** `HomeController.java` (lines 38-42), `HomeController.java` (lines 52-53), `librarian.html` (lines 755-782)
- **Issue:** Potential data synchronization issue between frontend and backend

### 2. Inconsistent Navigation
- **Librarian.html** uses custom non-navbar-fragment layout
- **Other templates** use shared `navbar.html` fragment
- **Impact:** Inconsistent user experience across library interfaces

### 3. Login Page UX Issues
- **Current behavior:** Shows "You have logged out successfully" message after logout
- **Expected behavior:** Should redirect to home page
- **Recommendation:** Fix redirect logic in `AuthController.java`

## UI/UX Issues

### 1. Template Inconsistency
- **Problem:** `librarian.html` uses custom layout instead of shared `navbar.html` fragment
- **Evidence:** Compare `home.html` (line 336) vs `librarian.html` (line 1)
- **Impact:** Fragmented user experience and maintenance overhead

### 2. Responsive Design Gaps
- **Mobile viewport:** Some elements don't adapt well to smaller screens
- **Evidence:** `home.html` media queries (lines 291-323) vs `librarian.html` (lines 431-466)
- **Recommendation:** Standardize responsive behavior across all templates

## Security Recommendations

1. **Immediate Actions:**
   - Remove hardcoded `LIBRARIAN_EMAILS` from `GoogleOidcUserService.java`
   - Fix `AuthAuditListener` to extract email from OIDC user info
   - Ensure `application-local.properties` is properly .gitignored

2. **Medium-Term:**
   - Implement database-backed role management
   - Add proper error handling for OIDC authentication failures
   - Implement secure credential management practices

3. **Long-Term:**
   - Audit all authentication flows for consistency
   - Implement comprehensive security testing
   - Conduct regular security code reviews

---

*Prepared by: LIBRA LMS Audit Team*
*Date: November 20, 2026*
*Status: Approved for review*