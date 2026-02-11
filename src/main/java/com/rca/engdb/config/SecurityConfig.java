package com.rca.engdb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for simpler API testing
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/databases.html", "/query.html", "/css/**", "/js/**").permitAll() // Allow static resources
                .requestMatchers("/api/query/**").permitAll() // Allow public access to query endpoints
                .requestMatchers("/api/**").authenticated() // Secure other API endpoints
                .anyRequest().authenticated()
            )
            .httpBasic(withDefaults()); // Use Basic Auth

        return http.build();
    }
}