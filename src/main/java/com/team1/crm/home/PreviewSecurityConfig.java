package com.team1.crm.home;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** DEV ONLY: Allows UI-only preview routes and static assets; not production authorization. */
@Configuration
@Profile("dev")
public class PreviewSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain previewPages(HttpSecurity http) throws Exception {
        return http.securityMatcher("/", "/index.html", "/admin", "/admin/**", "/analytics/dashboard",
                        "/customers", "/login", "/signup", "/css/**", "/js/**", "/images/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
