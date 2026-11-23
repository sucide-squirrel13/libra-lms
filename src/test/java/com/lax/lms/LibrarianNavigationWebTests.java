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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        HomeController.class,
        LibrarianBorrowingController.class,
        LibrarianUserController.class,
        AuditController.class
})
@Import({
        SecurityConfig.class,
        GlobalModelAdvice.class,
        org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class
})
class LibrarianNavigationWebTests {

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

    private LibraryUser librarianUser;

    @BeforeEach
    void setUp() {
        librarianUser = new LibraryUser();
        librarianUser.setFullName("Librarian Admin");
        librarianUser.setEmail("librarian@example.com");
        librarianUser.setRole("LIBRARIAN");
        ReflectionTestUtils.setField(librarianUser, "id", 10L);

        when(userRepository.findByEmailIgnoreCase("librarian@example.com")).thenReturn(Optional.of(librarianUser));
        when(dashboardService.getStatistics()).thenReturn(new LibrarianDashboardService.DashboardStats(10, 5, 2, 1, 3, 4));
        when(borrowingRepository.findAllByOrderByBorrowDateDesc()).thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(List.of(librarianUser));
        when(auditLogRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());
        when(bookRepository.findAll()).thenReturn(Collections.emptyList());
    }

    private static RequestPostProcessor librarianAuth() {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            org.springframework.security.core.userdetails.User principal =
                    new org.springframework.security.core.userdetails.User(
                            "librarian@example.com",
                            "password",
                            List.of(new SimpleGrantedAuthority("ROLE_LIBRARIAN")));
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    principal, "password", principal.getAuthorities()));
            request.getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            return request;
        };
    }

    @Test
    @DisplayName("GET /librarian renders modularized librarian navbar with active dashboard link and reading lamp")
    void getLibrarian_rendersModularNavbar() throws Exception {
        mockMvc.perform(get("/librarian").with(librarianAuth()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("librarian-navbar")))
                .andExpect(content().string(containsString("Libra <span class=\"gold-accent\">LMS</span>")))
                .andExpect(content().string(containsString("data-view=\"dashboard\"")))
                .andExpect(content().string(containsString("/librarian#books")))
                .andExpect(content().string(containsString("/librarian/borrowings")))
                .andExpect(content().string(containsString("/librarian/users")))
                .andExpect(content().string(containsString("/librarian/audit-logs")))
                .andExpect(content().string(containsString("theme-toggle-btn")))
                .andExpect(content().string(containsString("Librarian Admin")))
                .andExpect(content().string(containsString("LIBRARIAN")));
    }

    @Test
    @DisplayName("GET /librarian/borrowings renders modularized navbar with active borrowings link and theme toggle")
    void getBorrowings_rendersModularNavbarWithActiveBorrowings() throws Exception {
        mockMvc.perform(get("/librarian/borrowings").with(librarianAuth()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("librarian-navbar")))
                .andExpect(content().string(containsString("href=\"/librarian/borrowings\"")))
                .andExpect(content().string(containsString("theme-toggle-btn")))
                .andExpect(content().string(containsString("Borrowing Records")));
    }

    @Test
    @DisplayName("GET /librarian/users renders modularized navbar with active users link and theme toggle")
    void getUsers_rendersModularNavbarWithActiveUsers() throws Exception {
        mockMvc.perform(get("/librarian/users").with(librarianAuth()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("librarian-navbar")))
                .andExpect(content().string(containsString("href=\"/librarian/users\"")))
                .andExpect(content().string(containsString("theme-toggle-btn")))
                .andExpect(content().string(containsString("User Management")));
    }

    @Test
    @DisplayName("GET /librarian/audit-logs renders modularized navbar with active audit logs link and theme toggle")
    void getAuditLogs_rendersModularNavbarWithActiveAuditLogs() throws Exception {
        mockMvc.perform(get("/librarian/audit-logs").with(librarianAuth()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("librarian-navbar")))
                .andExpect(content().string(containsString("href=\"/librarian/audit-logs\"")))
                .andExpect(content().string(containsString("theme-toggle-btn")))
                .andExpect(content().string(containsString("Audit Logs")));
    }
}
