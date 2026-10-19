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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {BorrowingController.class, HomeController.class})
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
}
