package com.lax.lms;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {

            boolean isLibrarian = authentication.getAuthorities()
                    .stream()
                    .anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_LIBRARIAN"));

            if (isLibrarian) {
                response.sendRedirect("/librarian");
            } else {
                response.sendRedirect("/user/dashboard");
            }
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationSuccessHandler authenticationSuccessHandler,
            GoogleOidcUserService googleOidcUserService,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider)
            throws Exception {

        http.authorizeHttpRequests(authorize -> authorize

                .requestMatchers(
                        "/", "/register", "/login",
                        "/error", "/favicon.ico",
                        "/css/**", "/js/**", "/images/**"
                ).permitAll()

                .requestMatchers(
                        org.springframework.http.HttpMethod.POST,
                        "/books/*/borrow",
                        "/borrowings/*/return"
                ).hasRole("USER")

                .requestMatchers("/librarian/**")
                .hasRole("LIBRARIAN")

                .requestMatchers("/user/**")
                .hasRole("USER")

                .anyRequest().authenticated()
        );

        // Existing email/password login
        http.formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
        );

        // Google OAuth login
        http.oauth2Login(oauth2 -> {
            oauth2.loginPage("/login");
            oauth2.userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(googleOidcUserService)
            );
            oauth2.successHandler(authenticationSuccessHandler);
            oauth2.failureUrl("/login?oauth2error");

            ClientRegistrationRepository repo =
                    clientRegistrationRepositoryProvider.getIfAvailable();
            if (repo != null) {
                oauth2.authorizationEndpoint(authorization -> authorization
                        .authorizationRequestResolver(authorizationRequestResolver(repo))
                );
            }
        });

        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
        );

        return http.build();
    }

    OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {

        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        "/oauth2/authorization"
                );

        resolver.setAuthorizationRequestCustomizer(customizer ->
                customizer.additionalParameters(params ->
                        params.put("prompt", "select_account")
                )
        );

        return resolver;
    }
}
