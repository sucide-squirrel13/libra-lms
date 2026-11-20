# LIBRA LMS Implementation Roadmap

## Executive Summary
This roadmap outlines prioritized implementation tasks for LIBRA LMS, organized by priority level with affected files, dependencies, and testing requirements. All changes preserve the existing Spring Boot/Java/Thymeleaf/PostgreSQL architecture.

## Priority 1: Security Fixes (Immediate - Week 1)

### Task 1.1: Remove Hardcoded Librarian Allowlist
**Affected Files:**
- `GoogleOidcUserService.java` (lines 26-29)

**Dependencies:**
- Database-backed role management system

**Testing:**
- Unit tests for role assignment
- Integration tests for librarian login flows
- Verify existing librarian accounts retain access

### Task 1.2: Fix OIDC Authentication
**Affected Files:**
- `AuthAuditListener.java` (lines 19-27)
- `GoogleOidcUserService.java`

**Dependencies:**
- Proper OIDC email extraction logic

**Testing:**
- Unit tests for email extraction
- Integration tests for Google OAuth flows
- Verify authentication success/failure events

### Task 1.3: Secure Credential Management
**Affected Files:**
- `application-local.properties`
- `.gitignore`

**Dependencies:**
- Environment variable configuration

**Testing:**
- Verify .gitignore excludes local config files
- Ensure no credentials committed to repository

## Priority 2: UI/UX Standardization (Week 2-3)

### Task 2.1: Standardize Navigation Across Templates
**Affected Files:**
- `librarian.html` (convert to use navbar fragment)
- `librarian-users.html`
- `librarian-borrowings.html`
- `audit-logs.html`
- `add-book.html`
- `edit-book.html`
- `navbar.html` (fragments)

**Dependencies:**
- Existing navbar fragment structure

**Testing:**
- Verify all librarian templates use shared navigation
- Test mobile navigation consistency
- Confirm profile dropdown functionality across templates

### Task 2.2: Fix Login Page Redirect After Logout
**Affected Files:**
- `AuthController.java`

**Dependencies:**
- Logout success handling

**Testing:**
- Verify redirect to home page after logout
- Test session invalidation
- Confirm flash message display

### Task 2.3: Responsive Design Improvements
**Affected Files:**
- `main.css` (add media queries)
- `home.html`
- `user-dashboard.html`
- `user-borrowings.html`

**Dependencies:**
- CSS breakpoint system

**Testing:**
- Test on mobile, tablet, desktop viewports
- Verify grid system responsiveness
- Check touch target sizes for mobile

## Priority 3: Cinematic Design Enhancements (Week 4-8)

### Task 3.1: Implement GSAP ScrollTrigger System
**Affected Files:**
- `main.css` (animation classes)
- `navbar.js` (animation initialization)
- New: `js/cinematic-scroll.js`

**Dependencies:**
- GSAP library
- ScrollTrigger plugin

**Testing:**
- Verify scroll-triggered animations work correctly
- Test performance on different devices
- Confirm reduced motion preference support

### Task 3.2: 3D Book Display System with Three.js
**Affected Files:**
- `home.html` (featured books section)
- `user-dashboard.html` (book grid)
- New: `js/book-display.js`
- New: `css/book-3d.css`

**Dependencies:**
- Three.js library
- GSAP for motion

**Testing:**
- Test 3D rendering performance
- Verify book cover rotations
- Check mobile fallback for WebGL

### Task 3.3: Animated Typography System
**Affected Files:**
- `home.html` (hero section)
- `user-dashboard.html` (hero section)
- `main.css` (animation classes)

**Dependencies:**
- GSAP text animations
- Custom animation keyframes

**Testing:**
- Verify text animations render correctly
- Check readability during animations
- Test reduced motion preference

## Priority 4: Feature Enhancements (Week 9-12)

### Task 4.1: Interactive Book Displays
**Affected Files:**
- `book-catalogue.html`
- `home.html` (featured books)

**Dependencies:**
- Task 3.2 (3D book system)

**Testing:**
- Verify book interaction responsiveness
- Test hover and click states
- Confirm accessibility compliance

### Task 4.2: Cinematic Page Transitions
**Affected Files:**
- All Thymeleaf templates
- `js/navbar.js` (transition logic)

**Dependencies:**
- GSAP animation system

**Testing:**
- Test transition timing and smoothness
- Verify navigation state preservation
- Check browser back/forward behavior

## Timeline Overview

| Phase | Duration | Focus |
|-------|----------|-------|
| Phase 1 | Week 1 | Security fixes |
| Phase 2 | Weeks 2-3 | UI/UX standardization |
| Phase 3 | Weeks 4-8 | Cinematic enhancements |
| Phase 4 | Weeks 9-12 | Feature enhancements |

## Testing Requirements

### Unit Testing
- Test all modified Java controllers/services
- Verify authentication flows
- Confirm database operations

### Integration Testing
- Test end-to-end user flows
- Verify OAuth2 integration
- Confirm role-based access control

### Visual Testing
- Compare against 12 screenshots in `~/Pictures/Screenshots/`
- Verify cinematic elements match KAGE reference
- Test responsive layouts across viewports

### Security Testing
- Verify no credential exposure
- Test authentication boundaries
- Confirm authorization rules

### Performance Testing
- Measure animation frame rates
- Verify 3D rendering performance
- Test page load times

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Credential exposure | High | Ensure .gitignore, environment variables |
| Authentication failures | High | Comprehensive integration tests |
| Performance degradation | Medium | Lazy loading, mobile fallbacks |
| Browser compatibility | Medium | Test across browsers, progressive enhancement |
| Reduced motion violations | Low | Respect `prefers-reduced-motion` media query |

## Approval Required

Before implementation begins, approval is needed for:
1. Security fixes (Priority 1)
2. UI/UX standardization (Priority 2)
3. Cinematic design enhancements (Priority 3)
4. Feature enhancements (Priority 4)

---

*Prepared by: LIBRA LMS Implementation Team*
*Date: November 20, 2026*
*Status: Pending approval*