package com.lax.lms;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
                response.sendRedirect("/librarian/dashboard");
            } else {
                response.sendRedirect("/user/dashboard");
            }
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationSuccessHandler authenticationSuccessHandler)
            throws Exception {

        http.authorizeHttpRequests(authorize -> authorize

                .requestMatchers(
                        "/", "/register", "/login",
                        "/error", "/favicon.ico",
                        "/css/**", "/js/**", "/images/**"
                ).permitAll()

                .requestMatchers("/librarian/**")
                .hasRole("LIBRARIAN")

                .requestMatchers("/user/**")
                .hasRole("USER")

                .anyRequest().authenticated()
        );

        http.formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
        );

        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
        );

        return http.build();
    }
}