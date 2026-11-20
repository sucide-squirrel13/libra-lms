package com.lax.lms;

import com.lax.lms.repository.AuditLogRepository;
import com.lax.lms.repository.BookRepository;
import com.lax.lms.repository.BorrowingRepository;
import com.lax.lms.repository.LibraryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {BorrowingController.class, HomeController.class, LibrarianBorrowingController.class})
@Import({
        SecurityConfig.class,
        org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class
})
class BorrowingSecurityWebTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BorrowingService borrowingService;

    @MockitoBean
    private GoogleOidcUserService googleOidcUserService;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private BorrowingRepository borrowingRepository;

    @MockitoBean
    private LibraryUserRepository userRepository;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private LibrarianDashboardService dashboardService;

    private LibraryUser regularUser;
    private LibraryUser librarianUser;

    @BeforeEach
    void setUp() {
        regularUser = new LibraryUser();
        regularUser.setFullName("Regular Reader");
        regularUser.setEmail("reader@example.com");
        regularUser.setRole("USER");
        ReflectionTestUtils.setField(regularUser, "id", 1L);

        librarianUser = new LibraryUser();
        librarianUser.setFullName("Head Librarian");
        librarianUser.setEmail("librarian@example.com");
        librarianUser.setRole("LIBRARIAN");
        ReflectionTestUtils.setField(librarianUser, "id", 2L);

        when(userRepository.findByEmailIgnoreCase("reader@example.com")).thenReturn(Optional.of(regularUser));
        when(userRepository.findByEmailIgnoreCase("librarian@example.com")).thenReturn(Optional.of(librarianUser));
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

    private static RequestPostProcessor authenticatedUser(String email, String role) {
        return request -> {
            String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    email, "password", List.of(new SimpleGrantedAuthority(authority)));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            request.setUserPrincipal(auth);
            return request;
        };
    }

    @Test
    @DisplayName("Unauthenticated borrowing request is rejected with redirect to /login")
    void unauthenticatedBorrowingRequest_isRejected() throws Exception {
        mockMvc.perform(post("/books/1/borrow")
                        .with(validCsrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Unauthenticated return request is rejected with redirect to /login")
    void unauthenticatedReturnRequest_isRejected() throws Exception {
        mockMvc.perform(post("/borrowings/1/return")
                        .with(validCsrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("ROLE_USER with valid CSRF is allowed to borrow a book")
    void roleUser_canReachBorrowEndpoint_withValidCsrf() throws Exception {
        doNothing().when(borrowingService).borrowBook(anyLong(), anyLong());

        mockMvc.perform(post("/books/1/borrow")
                        .with(validCsrf())
                        .with(authenticatedUser("reader@example.com", "ROLE_USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/borrowings"));
    }

    @Test
    @DisplayName("ROLE_USER with valid CSRF is allowed to return a book")
    void roleUser_canReachReturnEndpoint_withValidCsrf() throws Exception {
        doNothing().when(borrowingService).returnBook(anyLong(), anyLong());

        mockMvc.perform(post("/borrowings/1/return")
                        .with(validCsrf())
                        .with(authenticatedUser("reader@example.com", "ROLE_USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/borrowings"));
    }

    @Test
    @DisplayName("ROLE_LIBRARIAN is denied borrowing with 403 Forbidden")
    void roleLibrarian_borrowBook_isDeniedWith403() throws Exception {
        mockMvc.perform(post("/books/1/borrow")
                        .with(validCsrf())
                        .with(authenticatedUser("librarian@example.com", "ROLE_LIBRARIAN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_LIBRARIAN is denied user-return operation with 403 Forbidden")
    void roleLibrarian_returnBook_isDeniedWith403() throws Exception {
        mockMvc.perform(post("/borrowings/1/return")
                        .with(validCsrf())
                        .with(authenticatedUser("librarian@example.com", "ROLE_LIBRARIAN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST request without valid CSRF token is rejected with 403 Forbidden")
    void postRequestWithoutValidCsrf_isForbidden() throws Exception {
        mockMvc.perform(post("/books/1/borrow")
                        .with(authenticatedUser("reader@example.com", "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER is denied access to librarian dashboard with 403 Forbidden")
    void regularUser_isDeniedLibrarianDashboard() throws Exception {
        mockMvc.perform(get("/librarian")
                        .with(authenticatedUser("reader@example.com", "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER is denied librarian book creation with 403 Forbidden")
    void regularUser_isDeniedLibrarianBookCreation() throws Exception {
        mockMvc.perform(post("/librarian/books")
                        .with(validCsrf())
                        .with(authenticatedUser("reader@example.com", "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_LIBRARIAN is allowed access to librarian dashboard")
    void librarian_isAllowedLibrarianDashboard() throws Exception {
        when(bookRepository.findAll()).thenReturn(Collections.emptyList());
        when(dashboardService.getStatistics()).thenReturn(
                new LibrarianDashboardService.DashboardStats(0, 0, 0, 0, 0, 0));

        mockMvc.perform(get("/librarian")
                        .with(authenticatedUser("librarian@example.com", "ROLE_LIBRARIAN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Return POST request without valid CSRF token is rejected with 403 Forbidden")
    void returnPostRequestWithoutValidCsrf_isForbidden() throws Exception {
        mockMvc.perform(post("/borrowings/1/return")
                        .with(authenticatedUser("reader@example.com", "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER is denied librarian book deletion with 403 Forbidden")
    void regularUser_isDeniedLibrarianBookDelete() throws Exception {
        mockMvc.perform(post("/librarian/books/delete/1")
                        .with(validCsrf())
                        .with(authenticatedUser("reader@example.com", "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER is denied librarian book update with 403 Forbidden")
    void regularUser_isDeniedLibrarianBookUpdate() throws Exception {
        mockMvc.perform(post("/librarian/books/update/1")
                        .with(validCsrf())
                        .with(authenticatedUser("reader@example.com", "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_LIBRARIAN can return book via /librarian/borrowings/{id}/return with valid CSRF")
    void librarian_returnBook_succeedsAndRedirects() throws Exception {
        doNothing().when(borrowingService).returnBookByLibrarian(1L, "librarian@example.com");

        mockMvc.perform(post("/librarian/borrowings/1/return")
                        .with(validCsrf())
                        .with(authenticatedUser("librarian@example.com", "ROLE_LIBRARIAN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/librarian/borrowings"))
                .andExpect(flash().attribute("message", "Book marked as returned successfully."));

        verify(borrowingService).returnBookByLibrarian(1L, "librarian@example.com");
    }

    @Test
    @DisplayName("ROLE_USER is denied access to /librarian/borrowings/{id}/return with 403 Forbidden")
    void regularUser_librarianReturnBook_isDeniedWith403() throws Exception {
        mockMvc.perform(post("/librarian/borrowings/1/return")
                        .with(validCsrf())
                        .with(authenticatedUser("reader@example.com", "ROLE_USER")))
                .andExpect(status().isForbidden());

        verify(borrowingService, never()).returnBookByLibrarian(anyLong(), anyString());
    }

    @Test
    @DisplayName("ROLE_LIBRARIAN return request without valid CSRF is rejected with 403 Forbidden")
    void librarian_returnBookWithoutCsrf_isForbidden() throws Exception {
        mockMvc.perform(post("/librarian/borrowings/1/return")
                        .with(authenticatedUser("librarian@example.com", "ROLE_LIBRARIAN")))
                .andExpect(status().isForbidden());

        verify(borrowingService, never()).returnBookByLibrarian(anyLong(), anyString());
    }

    @Test
    @DisplayName("ROLE_LIBRARIAN return request handles service error and redirects with error flash message")
    void librarian_returnBook_whenServiceThrows_redirectsWithErrorFlash() throws Exception {
        doThrow(new IllegalStateException("This book has already been returned."))
                .when(borrowingService).returnBookByLibrarian(1L, "librarian@example.com");

        mockMvc.perform(post("/librarian/borrowings/1/return")
                        .with(validCsrf())
                        .with(authenticatedUser("librarian@example.com", "ROLE_LIBRARIAN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/librarian/borrowings"))
                .andExpect(flash().attribute("errorMessage", "This book has already been returned."));

        verify(borrowingService).returnBookByLibrarian(1L, "librarian@example.com");
    }

    @Test
    @DisplayName("GET /librarian/borrowings renders Action column with Mark as Returned button and Completed badge")
    void librarian_viewBorrowings_rendersActionColumnAndBadges() throws Exception {
        Book book1 = new Book();
        book1.setTitle("Spring Security in Action");
        Borrowing activeBorrowing = new Borrowing();
        ReflectionTestUtils.setField(activeBorrowing, "id", 101L);
        activeBorrowing.setUser(regularUser);
        activeBorrowing.setBook(book1);
        activeBorrowing.setStatus("BORROWED");
        activeBorrowing.setBorrowDate(LocalDate.now().minusDays(5));
        activeBorrowing.setDueDate(LocalDate.now().plusDays(9));

        Book book2 = new Book();
        book2.setTitle("Effective Java");
        Borrowing returnedBorrowing = new Borrowing();
        ReflectionTestUtils.setField(returnedBorrowing, "id", 102L);
        returnedBorrowing.setUser(regularUser);
        returnedBorrowing.setBook(book2);
        returnedBorrowing.setStatus("RETURNED");
        returnedBorrowing.setBorrowDate(LocalDate.now().minusDays(15));
        returnedBorrowing.setDueDate(LocalDate.now().minusDays(1));
        returnedBorrowing.setReturnDate(LocalDate.now().minusDays(1));

        when(borrowingRepository.findAllByOrderByBorrowDateDesc())
                .thenReturn(List.of(activeBorrowing, returnedBorrowing));

        mockMvc.perform(get("/librarian/borrowings")
                        .with(authenticatedUser("librarian@example.com", "ROLE_LIBRARIAN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<th>Action</th>")))
                .andExpect(content().string(containsString("Mark as Returned")))
                .andExpect(content().string(containsString("/librarian/borrowings/101/return")))
                .andExpect(content().string(containsString("Completed")));
    }

    @Test
    @DisplayName("GET / as guest renders public navigation with login link and catalogue browsing")
    void guest_publicHome_rendersPublicNavigation() throws Exception {
        when(bookRepository.findAll()).thenReturn(Collections.emptyList());
        when(bookRepository.count()).thenReturn(0L);
        when(bookRepository.countTotalCopies()).thenReturn(0L);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/books\"")))
                .andExpect(content().string(containsString("Browse Catalogue")))
                .andExpect(content().string(containsString("href=\"/login\"")))
                .andExpect(content().string(not(containsString("Librarian Workspace"))))
                .andExpect(content().string(not(containsString("Librarian Dashboard"))))
                .andExpect(content().string(not(containsString("href=\"/user/borrowings\""))))
                .andExpect(content().string(not(containsString("href=\"/user/dashboard\""))));
    }

    @Test
    @DisplayName("GET / as student renders student navigation with Dashboard and student quick links")
    void student_publicHome_rendersStudentNavigation() throws Exception {
        when(bookRepository.findAll()).thenReturn(Collections.emptyList());
        when(bookRepository.count()).thenReturn(0L);
        when(bookRepository.countTotalCopies()).thenReturn(0L);

        mockMvc.perform(get("/")
                        .with(authenticatedUser("reader@example.com", "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/books\"")))
                .andExpect(content().string(containsString("href=\"/user/dashboard\"")))
                .andExpect(content().string(containsString("href=\"/user/borrowings\"")))
                .andExpect(content().string(containsString("My Borrowings")))
                .andExpect(content().string(containsString("Personal Dashboard")))
                .andExpect(content().string(not(containsString("Librarian Dashboard"))))
                .andExpect(content().string(not(containsString("Librarian Workspace"))));
    }

    @Test
    @DisplayName("GET / as librarian renders Librarian Dashboard in navbar and Librarian Workspace in hero")
    void librarian_publicHome_rendersLibrarianNavigation() throws Exception {
        when(bookRepository.findAll()).thenReturn(Collections.emptyList());
        when(bookRepository.count()).thenReturn(0L);
        when(bookRepository.countTotalCopies()).thenReturn(0L);

        mockMvc.perform(get("/")
                        .with(authenticatedUser("librarian@example.com", "ROLE_LIBRARIAN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/books\"")))
                .andExpect(content().string(containsString("href=\"/librarian\"")))
                .andExpect(content().string(containsString("Librarian Dashboard")))
                .andExpect(content().string(containsString("Librarian Workspace")))
                .andExpect(content().string(not(containsString("href=\"/user/borrowings\""))))
                .andExpect(content().string(not(containsString("href=\"/user/dashboard\""))))
                .andExpect(content().string(not(containsString("Personal Dashboard"))));
    }

    @Test
    @DisplayName("GET /oauth2/authorization/google redirects to Google with prompt=select_account")
    void oauth2AuthorizationGoogle_redirectsWithPromptSelectAccount() throws Exception {
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

        when(clientRegistrationRepository.findByRegistrationId("google")).thenReturn(registration);

        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("prompt=select_account")))
                .andExpect(header().string("Location", containsString("https://accounts.google.com/o/oauth2/v2/auth")))
                .andExpect(header().string("Location", containsString("client_id=test-client-id")));
    }
}
