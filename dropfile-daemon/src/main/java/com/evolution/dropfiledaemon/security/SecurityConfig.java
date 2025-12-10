package com.evolution.dropfiledaemon.security;

import com.evolution.dropfiledaemon.handshake.security.RequireHandshakeAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain security(HttpSecurity http,
                                        ApiAuthFilter apiAuthFilter,
                                        RequireHandshakeAuthFilter requireHandshakeAuthFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(apiAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorizeHttpRequestsCustomizer -> {
                    apiAuthFilter
                            .getPathPatterns()
                            .forEach(pattern -> authorizeHttpRequestsCustomizer
                                    .requestMatchers(pattern)
                                    .permitAll()
                            );
                })
                .addFilterAfter(requireHandshakeAuthFilter, ApiAuthFilter.class)
                .authorizeHttpRequests(authorizeHttpRequestsCustomizer -> {
                    requireHandshakeAuthFilter
                            .getPathPatterns()
                            .forEach(pattern -> authorizeHttpRequestsCustomizer
                                    .requestMatchers(pattern)
                                    .permitAll()
                            );
                })
                .authorizeHttpRequests(it -> {
                    it.anyRequest().permitAll();
                })
                .build();
    }
}