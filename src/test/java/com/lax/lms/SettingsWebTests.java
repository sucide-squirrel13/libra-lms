package com.lax.lms;

import com.lax.lms.repository.LibraryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SettingsController.class)
@Import({
        SecurityConfig.class,
        GlobalModelAdvice.class,
        org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class
})
class SettingsWebTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibraryUserRepository userRepository;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private GoogleOidcUserService googleOidcUserService;

    private LibraryUser testUser;
    private LibraryUser librarianUser;

    @BeforeEach
    void setUp() {
        testUser = new LibraryUser();
        ReflectionTestUtils.setField(testUser, "id", 101L);
        testUser.setFullName("Ada Lovelace");
        testUser.setEmail("ada@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole("USER");
        testUser.setThemePreference("dark");
        testUser.setAccentColor("gold");

        librarianUser = new LibraryUser();
        ReflectionTestUtils.setField(librarianUser, "id", 202L);
        librarianUser.setFullName("Grace Hopper");
        librarianUser.setEmail("grace@example.com");
        librarianUser.setPassword("encodedPassword");
        librarianUser.setRole("LIBRARIAN");
        librarianUser.setThemePreference("light");
        librarianUser.setAccentColor("bronze");
    }

    private static RequestPostProcessor validCsrf() {
        return request -> {
            MockHttpServletResponse response = new MockHttpServletResponse();
            HttpSessionCsrfTokenRepository repo = new HttpSessionCsrfTokenRepository();
            XorCsrfTokenRequestAttributeHandler handler = new XorCsrfTokenRequestAttributeHandler();
            CsrfToken token = repo.generateToken(request);
            repo.saveToken(token, request, response);
            handler.handle(request, response, () -> token);
            CsrfToken attrToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            request.addParameter(attrToken.getParameterName(), attrToken.getToken());
            return request;
        };
    }

    private RequestPostProcessor authenticatedAs(String email, String role) {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    email,
                    "password",
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            request.getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );
            return request;
        };
    }

    @Test
    @DisplayName("Unauthenticated request to /settings redirects to login")
    void unauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/settings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Authenticated student can access /settings and view interactive theme and preferences")
    void studentCanAccessSettings() throws Exception {
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/settings").with(authenticatedAs("ada@example.com", "USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sanctuary Settings")))
                .andExpect(content().string(containsString("theme-toggle-btn")))
                .andExpect(content().string(containsString("Theme Mode")))
                .andExpect(content().string(containsString("Reading Font Scale")));
    }

    @Test
    @DisplayName("Authenticated librarian can access /settings with librarian navigation")
    void librarianCanAccessSettings() throws Exception {
        when(userRepository.findByEmailIgnoreCase("grace@example.com")).thenReturn(Optional.of(librarianUser));

        mockMvc.perform(get("/settings").with(authenticatedAs("grace@example.com", "LIBRARIAN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("librarian-nav")))
                .andExpect(content().string(containsString("Sanctuary Settings")));
    }

    @Test
    @DisplayName("Updating preferences persists to repository and redirects")
    void updatePreferencesSavesAndRedirects() throws Exception {
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/settings/preferences")
                        .with(authenticatedAs("ada@example.com", "USER"))
                        .with(validCsrf())
                        .param("themePreference", "light")
                        .param("accentColor", "emerald")
                        .param("interfaceDensity", "compact")
                        .param("fontSizePreference", "large")
                        .param("contentWidth", "wide")
                        .param("readingLayout", "grid")
                        .param("emailBorrowReminders", "true")
                        .param("reducedMotionPreference", "false")
                        .param("highContrastPreference", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"))
                .andExpect(flash().attributeExists("settingsSuccess"));

        verify(userRepository).save(any(LibraryUser.class));
    }

    @Test
    @DisplayName("AJAX theme toggle persists theme preference immediately")
    void ajaxThemeToggleSucceeds() throws Exception {
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/settings/theme")
                        .with(authenticatedAs("ada@example.com", "USER"))
                        .with(validCsrf())
                        .param("theme", "light"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.theme").value("light"));

        verify(userRepository).save(any(LibraryUser.class));
    }
}
