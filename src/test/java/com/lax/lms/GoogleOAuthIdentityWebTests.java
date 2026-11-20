package com.lax.lms;

import com.lax.lms.repository.AuditLogRepository;
import com.lax.lms.repository.BookRepository;
import com.lax.lms.repository.BorrowingRepository;
import com.lax.lms.repository.LibraryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression tests for SEC-02 / SEC-03: controllers and the global advice must
 * resolve the account email for BOTH authentication strategies.
 *
 * Form login: the principal is a UserDetails whose username is the stored
 * account email (see LibraryUserDetailsService).
 *
 * Google OIDC: the principal is a DefaultOidcUser whose name attribute is
 * "sub", so Authentication.getName() is the OIDC subject - NOT the email.
 * The email claim must be used instead.
 */
@WebMvcTest(controllers = {BorrowingController.class, ProfileController.class, UserController.class})
@Import({
        SecurityConfig.class,
        org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class
})
class GoogleOAuthIdentityWebTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileController profileController;

    @TempDir
    Path tempDir;

    @MockitoBean
    private BorrowingService borrowingService;

    @MockitoBean
    private BorrowingRepository borrowingRepository;

    @MockitoBean
    private LibraryUserRepository userRepository;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private GoogleOidcUserService googleOidcUserService;

    @MockitoBean
    private LibrarianDashboardService dashboardService;

    private LibraryUser regularUser;

    @BeforeEach
    void setUp() {
        profileController.setUploadDirectory(tempDir);

        regularUser = new LibraryUser();
        regularUser.setFullName("Regular Reader");
        regularUser.setEmail("reader@example.com");
        regularUser.setRole("USER");
        ReflectionTestUtils.setField(regularUser, "id", 1L);

        when(userRepository.findByEmailIgnoreCase("reader@example.com"))
                .thenReturn(Optional.of(regularUser));
        when(userRepository.findByEmailIgnoreCase("google-subject-123"))
                .thenReturn(Optional.empty());
    }

    private static OAuth2AuthenticationToken oidcAuthentication() {
        return oidcAuthentication("reader@example.com");
    }

    private static OAuth2AuthenticationToken oidcAuthentication(String email) {
        Map<String, Object> claims = Map.of(
                "sub", "google-subject-123",
                "email", email,
                "email_verified", true,
                "name", "Regular Reader");
        Instant now = Instant.now();
        OidcIdToken idToken =
                new OidcIdToken("id-token-value", now, now.plusSeconds(3600), claims);
        OidcUserInfo userInfo = new OidcUserInfo(claims);
        DefaultOidcUser principal = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, userInfo, "sub");
        return new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google");
    }

    private static RequestPostProcessor formLoginUser() {
        return request -> {
            UserDetails principal = new User(
                    "reader@example.com",
                    "password",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            principal, "password", principal.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            request.getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            request.setUserPrincipal(auth);
            return request;
        };
    }

    private static RequestPostProcessor oidcUser() {
        return oidcUser("reader@example.com");
    }

    private static RequestPostProcessor oidcUser(String email) {
        return request -> {
            OAuth2AuthenticationToken auth = oidcAuthentication(email);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            request.getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            request.setUserPrincipal(auth);
            return request;
        };
    }

    private static RequestPostProcessor validCsrf() {
        return request -> {
            org.springframework.mock.web.MockHttpServletResponse response =
                    new org.springframework.mock.web.MockHttpServletResponse();
            HttpSessionCsrfTokenRepository repo = new HttpSessionCsrfTokenRepository();
            org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler handler =
                    new org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler();
            CsrfToken token = repo.generateToken(request);
            repo.saveToken(token, request, response);
            handler.handle(request, response, () -> token);
            CsrfToken attrToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            request.addParameter(attrToken.getParameterName(), attrToken.getToken());
            return request;
        };
    }

    @Test
    @DisplayName("SEC-02: form-login user can borrow a book")
    void borrowBook_formLoginUser_isIdentifiedByEmail() throws Exception {
        doNothing().when(borrowingService).borrowBook(anyLong(), anyLong());

        mockMvc.perform(post("/books/1/borrow")
                        .with(validCsrf())
                        .with(formLoginUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/borrowings"));

        verify(borrowingService).borrowBook(1L, 1L);
    }

    @Test
    @DisplayName("SEC-02: Google OIDC user can borrow a book (email claim, not sub)")
    void borrowBook_oidcUser_isIdentifiedByEmailClaim() throws Exception {
        doNothing().when(borrowingService).borrowBook(anyLong(), anyLong());

        mockMvc.perform(post("/books/1/borrow")
                        .with(validCsrf())
                        .with(oidcUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/borrowings"));

        verify(borrowingService).borrowBook(1L, 1L);
    }

    @Test
    @DisplayName("SEC-02: Google OIDC user can return a book (email claim, not sub)")
    void returnBook_oidcUser_isIdentifiedByEmailClaim() throws Exception {
        doNothing().when(borrowingService).returnBook(anyLong(), anyLong());

        mockMvc.perform(post("/borrowings/1/return")
                        .with(validCsrf())
                        .with(oidcUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/borrowings"));

        verify(borrowingService).returnBook(1L, 1L);
    }

    @Test
    @DisplayName("SEC-02: form-login user can return a book")
    void returnBook_formLoginUser_isIdentifiedByEmail() throws Exception {
        doNothing().when(borrowingService).returnBook(anyLong(), anyLong());

        mockMvc.perform(post("/borrowings/1/return")
                        .with(validCsrf())
                        .with(formLoginUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/borrowings"));

        verify(borrowingService).returnBook(1L, 1L);
    }

    @Test
    @DisplayName("SEC-02: OIDC email claim with surrounding whitespace is normalized")
    void borrowBook_oidcUser_withWhitespaceEmail_isTrimmed() throws Exception {
        doNothing().when(borrowingService).borrowBook(anyLong(), anyLong());

        mockMvc.perform(post("/books/1/borrow")
                        .with(validCsrf())
                        .with(oidcUser("  reader@example.com  ")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/borrowings"));

        verify(borrowingService).borrowBook(1L, 1L);
        verify(userRepository, atLeastOnce())
                .findByEmailIgnoreCase("reader@example.com");
        verify(userRepository, never())
                .findByEmailIgnoreCase("  reader@example.com  ");
    }

    @Test
    @DisplayName("SEC-03: profile photo lookup for OIDC user is not rejected as unauthenticated")
    void getProfilePhoto_oidcUser_notUnauthorized() throws Exception {
        // No uploads directory exists in the test environment, so the file is
        // deterministically absent -> 404. Before the fix, the OIDC principal
        // resolved to null and the response was 401.
        mockMvc.perform(get("/profile/photo")
                        .with(oidcUser()))
                .andExpect(status().isNotFound());

        verify(userRepository, atLeastOnce())
                .findByEmailIgnoreCase("reader@example.com");
        verify(userRepository, never())
                .findByEmailIgnoreCase("google-subject-123");
    }

    @Test
    @DisplayName("SEC-03: profile photo lookup for form-login user behaves the same")
    void getProfilePhoto_formLoginUser_notUnauthorized() throws Exception {
        mockMvc.perform(get("/profile/photo")
                        .with(formLoginUser()))
                .andExpect(status().isNotFound());

        verify(userRepository, atLeastOnce())
                .findByEmailIgnoreCase("reader@example.com");
        verify(userRepository, never())
                .findByEmailIgnoreCase("google-subject-123");
    }

    @Test
    @DisplayName("SEC-03: profile photo lookup for anonymous user redirects to /login")
    void getProfilePhoto_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile/photo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("SEC-03: empty photo upload for OIDC user reaches the same validation as form login")
    void uploadProfilePhoto_emptyFile_oidcUser_sameAsFormLogin() throws Exception {
        MockMultipartFile emptyPhoto = new MockMultipartFile(
                "photo", "photo.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/profile/photo")
                        .file(emptyPhoto)
                        .with(validCsrf())
                        .with(oidcUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dashboard"))
                .andExpect(flash().attribute(
                        "profileError", "Please choose a picture first."));
    }

    @Test
    @DisplayName("SEC-03: empty photo upload for form-login user reaches the same validation")
    void uploadProfilePhoto_emptyFile_formLoginUser_sameAsFormLogin() throws Exception {
        MockMultipartFile emptyPhoto = new MockMultipartFile(
                "photo", "photo.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/profile/photo")
                        .file(emptyPhoto)
                        .with(validCsrf())
                        .with(formLoginUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dashboard"))
                .andExpect(flash().attribute(
                        "profileError", "Please choose a picture first."));
    }

    @Test
    @DisplayName("user borrowings page renders active and overdue borrowing rows with correct classification and book titles")
    void userBorrowings_rendersActiveAndOverdueRows_withCorrectClassification() throws Exception {
        Book activeBook = new Book();
        activeBook.setTitle("Designing Data-Intensive Applications");
        activeBook.setCategory("Technology");

        Borrowing activeBorrowing = new Borrowing();
        ReflectionTestUtils.setField(activeBorrowing, "id", 101L);
        activeBorrowing.setUser(regularUser);
        activeBorrowing.setBook(activeBook);
        activeBorrowing.setBorrowDate(LocalDate.now().minusDays(3));
        activeBorrowing.setDueDate(LocalDate.now().plusDays(11));
        activeBorrowing.setStatus("BORROWED");

        Book overdueBook = new Book();
        overdueBook.setTitle("Clean Code");
        overdueBook.setCategory("Software");

        Borrowing overdueBorrowing = new Borrowing();
        ReflectionTestUtils.setField(overdueBorrowing, "id", 102L);
        overdueBorrowing.setUser(regularUser);
        overdueBorrowing.setBook(overdueBook);
        overdueBorrowing.setBorrowDate(LocalDate.now().minusDays(20));
        overdueBorrowing.setDueDate(LocalDate.now().minusDays(6));
        overdueBorrowing.setStatus("BORROWED");

        when(borrowingRepository.findByUserIdOrderByBorrowDateDesc(1L))
                .thenReturn(List.of(activeBorrowing, overdueBorrowing));

        mockMvc.perform(get("/user/borrowings")
                        .with(formLoginUser()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Designing Data-Intensive Applications")))
                .andExpect(content().string(containsString("Clean Code")))
                .andExpect(content().string(containsString("data-status=\"active\"")))
                .andExpect(content().string(containsString("data-status=\"overdue\"")))
                .andExpect(content().string(containsString("● Active Loan")))
                .andExpect(content().string(containsString("⚠ Overdue")));
    }

    @Test
    @DisplayName("user borrowings page renders correctly for Google OIDC user")
    void userBorrowings_oidcUser_rendersBorrowingRows() throws Exception {
        Book book = new Book();
        book.setTitle("Effective Java");
        book.setCategory("Programming");

        Borrowing borrowing = new Borrowing();
        ReflectionTestUtils.setField(borrowing, "id", 103L);
        borrowing.setUser(regularUser);
        borrowing.setBook(book);
        borrowing.setBorrowDate(LocalDate.now().minusDays(2));
        borrowing.setDueDate(LocalDate.now().plusDays(12));
        borrowing.setStatus("BORROWED");

        when(borrowingRepository.findByUserIdOrderByBorrowDateDesc(1L))
                .thenReturn(List.of(borrowing));

        mockMvc.perform(get("/user/borrowings")
                        .with(oidcUser()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Effective Java")))
                .andExpect(content().string(containsString("data-status=\"active\"")))
                .andExpect(content().string(containsString("● Active Loan")));
    }

    @Test
    @DisplayName("Google OAuth authorization request resolver includes prompt=select_account parameter")
    void googleOAuthAuthorizationRequest_containsPromptSelectAccount() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .build();

        InMemoryClientRegistrationRepository repo =
                new InMemoryClientRegistrationRepository(registration);

        OAuth2AuthorizationRequestResolver resolver =
                new SecurityConfig().authorizationRequestResolver(repo);

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        request.setServletPath("/oauth2/authorization/google");

        OAuth2AuthorizationRequest authRequest = resolver.resolve(request);

        assertThat(authRequest).isNotNull();
        assertThat(authRequest.getAdditionalParameters())
                .containsEntry("prompt", "select_account");
        assertThat(authRequest.getAuthorizationRequestUri())
                .contains("prompt=select_account");
        assertThat(authRequest.getScopes())
                .containsExactlyInAnyOrder("openid", "profile", "email");
        assertThat(authRequest.getClientId())
                .isEqualTo("test-client-id");
        assertThat(authRequest.getRedirectUri())
                .contains("/login/oauth2/code/google");
    }
}
