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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        HomeController.class,
        LibrarianBorrowingController.class,
        LibrarianUserController.class,
        AuditController.class,
        SettingsController.class,
        AuthController.class
})
@Import({
        SecurityConfig.class,
        org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class
})
class LibrarianRoleSecurityWebTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleOidcUserService googleOidcUserService;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private LibraryUserRepository userRepository;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private BorrowingRepository borrowingRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private LibrarianDashboardService dashboardService;

    @MockitoBean
    private BorrowingService borrowingService;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    private LibraryUser regularStudent;
    private LibraryUser authorizedLibrarian;

    @BeforeEach
    void setUp() {
        regularStudent = new LibraryUser();
        regularStudent.setFullName("Regular Scholar");
        regularStudent.setEmail("student@example.com");
        regularStudent.setRole("USER");
        ReflectionTestUtils.setField(regularStudent, "id", 10L);

        authorizedLibrarian = new LibraryUser();
        authorizedLibrarian.setFullName("Lakson Librarian");
        authorizedLibrarian.setEmail("lakson130208@gmail.com");
        authorizedLibrarian.setRole("LIBRARIAN");
        ReflectionTestUtils.setField(authorizedLibrarian, "id", 11L);

        when(userRepository.findByEmailIgnoreCase("student@example.com"))
                .thenReturn(Optional.of(regularStudent));
        when(userRepository.findByEmailIgnoreCase("lakson130208@gmail.com"))
                .thenReturn(Optional.of(authorizedLibrarian));

        when(bookRepository.findAll()).thenReturn(Collections.emptyList());
        when(borrowingRepository.findAll()).thenReturn(Collections.emptyList());
        when(auditLogRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());
        when(dashboardService.getStatistics()).thenReturn(
                new LibrarianDashboardService.DashboardStats(0L, 0L, 0L, 0L, 0L, 0L)
        );
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

    private static RequestPostProcessor authenticatedAs(String email, String role) {
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
    @DisplayName("Students with ROLE_USER are forbidden from accessing /librarian workspace")
    void student_forbiddenFromLibrarianWorkspace() throws Exception {
        mockMvc.perform(get("/librarian")
                        .with(authenticatedAs("student@example.com", "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Students with ROLE_USER are forbidden from accessing /librarian/borrowings")
    void student_forbiddenFromLibrarianBorrowings() throws Exception {
        mockMvc.perform(get("/librarian/borrowings")
                        .with(authenticatedAs("student@example.com", "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Students with ROLE_USER are forbidden from accessing /librarian/users")
    void student_forbiddenFromLibrarianUsers() throws Exception {
        mockMvc.perform(get("/librarian/users")
                        .with(authenticatedAs("student@example.com", "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Students with ROLE_USER are forbidden from accessing /librarian/audit-logs")
    void student_forbiddenFromLibrarianAuditLogs() throws Exception {
        mockMvc.perform(get("/librarian/audit-logs")
                        .with(authenticatedAs("student@example.com", "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated requests to /librarian routes redirect to /login")
    void unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/librarian"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Authorized librarian with ROLE_LIBRARIAN can access /librarian workspace")
    void authorizedLibrarian_canAccessLibrarianWorkspace() throws Exception {
        mockMvc.perform(get("/librarian")
                        .with(authenticatedAs("lakson130208@gmail.com", "ROLE_LIBRARIAN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cinematic-motion.js")))
                .andExpect(content().string(containsString("ScrollTrigger.min.js")));
    }

    @Test
    @DisplayName("Authorized librarian with ROLE_LIBRARIAN can access /librarian/borrowings")
    void authorizedLibrarian_canAccessBorrowings() throws Exception {
        when(borrowingRepository.findAll()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/librarian/borrowings")
                        .with(authenticatedAs("lakson130208@gmail.com", "ROLE_LIBRARIAN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cinematic-motion.js")))
                .andExpect(content().string(containsString("ScrollTrigger.min.js")));
    }

    @Test
    @DisplayName("Authorized librarian with ROLE_LIBRARIAN can access /librarian/users")
    void authorizedLibrarian_canAccessUsers() throws Exception {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/librarian/users")
                        .with(authenticatedAs("lakson130208@gmail.com", "ROLE_LIBRARIAN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cinematic-motion.js")))
                .andExpect(content().string(containsString("ScrollTrigger.min.js")));
    }

    @Test
    @DisplayName("Authorized librarian with ROLE_LIBRARIAN can access /librarian/audit-logs")
    void authorizedLibrarian_canAccessAuditLogs() throws Exception {
        when(auditLogRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/librarian/audit-logs")
                        .with(authenticatedAs("lakson130208@gmail.com", "ROLE_LIBRARIAN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cinematic-motion.js")))
                .andExpect(content().string(containsString("ScrollTrigger.min.js")));
    }

    @Test
    @DisplayName("Students cannot promote themselves by POSTing role=LIBRARIAN to /settings/preferences")
    void student_cannotPromoteSelfViaPreferences() throws Exception {
        mockMvc.perform(post("/settings/preferences")
                        .with(authenticatedAs("student@example.com", "ROLE_USER"))
                        .with(validCsrf())
                        .param("role", "LIBRARIAN")
                        .param("themePreference", "light")
                        .param("accentColor", "gold"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"));

        // Verify that the user's role remains USER
        assertThat(regularStudent.getRole()).isEqualTo("USER");
        verify(userRepository).save(regularStudent);
    }

    @Test
    @DisplayName("Students cannot promote themselves by POSTing role=LIBRARIAN to /settings/account")
    void student_cannotPromoteSelfViaAccount() throws Exception {
        mockMvc.perform(post("/settings/account")
                        .with(authenticatedAs("student@example.com", "ROLE_USER"))
                        .with(validCsrf())
                        .param("fullName", "Renamed Scholar")
                        .param("role", "LIBRARIAN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"));

        // Verify that the user's role remains USER
        assertThat(regularStudent.getRole()).isEqualTo("USER");
        assertThat(regularStudent.getFullName()).isEqualTo("Renamed Scholar");
        verify(userRepository).save(regularStudent);
    }

    @Test
    @DisplayName("Registration rejects or ignores role parameter and always calls register with USER role")
    void registration_ignoresRoleParameter() throws Exception {
        mockMvc.perform(post("/register")
                        .with(validCsrf())
                        .param("fullName", "New User")
                        .param("email", "newuser@example.com")
                        .param("password", "Password123!")
                        .param("role", "LIBRARIAN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(registrationService).register("New User", "newuser@example.com", "Password123!");
    }

    @Test
    @DisplayName("Registration with librarian email delegates to register service and does not grant librarian access")
    void registrationWithLibrarianEmail_doesNotGrantLibrarianAccess() throws Exception {
        mockMvc.perform(post("/register")
                        .with(validCsrf())
                        .param("fullName", "Lakson Registrant")
                        .param("email", "lakson130208@gmail.com")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(registrationService).register("Lakson Registrant", "lakson130208@gmail.com", "Password123!");
    }
}
